package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.CoverageCompat;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarkdownPrinterTest {

  Issue issue;
  Issue coverageIssue;

  List<Issue> report = new ArrayList<>();
  DummyIssuePathResolver issuePathResolver = new DummyIssuePathResolver();

  private static final String SONAR_URL = "sonarqube/URL";
  private static final String STASH_URL = "stash/URL";
  private MarkdownPrinter printer;
  private int issueThreshold;
  private Project project;

  PullRequestRef pr = PullRequestRef.builder()
      .setProject("stashProject")
      .setRepository("stashRepo")
      .setPullRequestId(1)
      .build();

  @Before
  public void setUp() {
    Issue issueBlocker = new DefaultIssue().setKey("key1")
        .setSeverity(Severity.BLOCKER)
        .setMessage("messageBlocker")
        .setRuleKey(RuleKey.of("RepoBlocker", "RuleBlocker"))
        .setLine(1);
    Issue issueCritical = new DefaultIssue().setKey("key2")
        .setSeverity(Severity.CRITICAL)
        .setMessage("messageCritical")
        .setRuleKey(RuleKey.of("RepoCritical", "RuleCritical"))
        .setLine(1);
    Issue issueMajor = new DefaultIssue().setKey("key3")
        .setSeverity(Severity.MAJOR)
        .setMessage("messageMajor")
        .setRuleKey(RuleKey.of("RepoMajor", "RuleMajor"))
        .setLine(1);

    report.add(issueBlocker);
    report.add(issueCritical);
    report.add(issueMajor);

    issue = issueBlocker;
    coverageIssue = new DefaultIssue().setKey("key4")
        .setSeverity(Severity.MAJOR)
        .setRuleKey(RuleKey.of(CoverageCompat.coverageEvolutionRepository("java"), "bla"))
        .setMessage("some text");

    report.add(coverageIssue);

    issuePathResolver = new DummyIssuePathResolver();
    issuePathResolver.add(coverageIssue, "path/code/coverage");

    issueThreshold = 100;

    project = new Project("project");
    printer = new MarkdownPrinter(STASH_URL, pr, issuePathResolver, issueThreshold, SONAR_URL);
  }

  @Test
  public void testPrintIssueMarkdown() {
    assertEquals(
        "*BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)]",
        printer.printIssueMarkdown(report.get(0))
    );
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdown() {
    assertEquals(
        "| BLOCKER | 1 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER")
    );

    assertEquals(
        "| MAJOR | 2 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR")
    );

    assertEquals(
        "| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO")
    );
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoIssues() {
    List<Issue> report = new ArrayList<>();

    assertEquals("| BLOCKER | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER"));
    assertEquals("| CRITICAL | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "CRITICAL"));
    assertEquals("| MAJOR | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR"));
    assertEquals("| MINOR | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MINOR"));
    assertEquals("| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoSonarQubeIssues() {
    List<Issue> report = new ArrayList<>();
    report.add(coverageIssue);

    assertEquals("| BLOCKER | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER"));
    assertEquals("| MAJOR | 1 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR"));
    assertEquals("| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoCoverageIssues() {
    report.remove(coverageIssue);

    assertEquals("| BLOCKER | 1 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER"));
    assertEquals("| MAJOR | 1 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR"));
    assertEquals("| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO"));
  }

  @Test
  public void testPrintIssueListBySeverityMarkdown() {
    report.remove(coverageIssue);

    assertEquals(
        "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n",
        printer.printIssueListBySeverityMarkdown(report, "BLOCKER"));

    assertEquals(
        "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n",
        printer.printIssueListBySeverityMarkdown(report, "CRITICAL"));

    assertEquals(
        "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n",
        printer.printIssueListBySeverityMarkdown(report, "MAJOR"));
  }

  @Test
  public void testPrintReportMarkdown() {
    String issueReportMarkdown = printer.printReportMarkdown(report, project);
    String reportString = "## SonarQube analysis Overview\n"
        + "| Total New Issues | 4 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 2 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n\n\n"
        + "| Coverage |\n"
        + "|---------------|\n"
        + "| *MAJOR* - some text [[coverageEvolution-java:bla](sonarqube/URL/coding_rules#rule_key=coverageEvolution-java:bla)] |"
        + "\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithIssueLimitation() {
    printer = new MarkdownPrinter(STASH_URL, pr, issuePathResolver, 3, SONAR_URL);
    String issueReportMarkdown = printer.printReportMarkdown(report, project);
    String reportString = "## SonarQube analysis Overview\n"
        + "### Too many issues detected (4/3): Issues cannot be displayed in Diff view.\n\n"
        + "| Total New Issues | 4 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 2 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n\n\n"
        + "| Coverage |\n"
        + "|---------------|\n"
        + "| *MAJOR* - some text [[coverageEvolution-java:bla](sonarqube/URL/coding_rules#rule_key=coverageEvolution-java:bla)] |"
        + "\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintEmptyReportMarkdown() {
    report = new ArrayList<>();

    String issueReportMarkdown = printer.printReportMarkdown(report, project);
    String reportString = "## SonarQube analysis Overview\n"
        + "### No new issues detected!\n\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithEmptySonarQubeReportAndWithLoweredIssues() {
    report = new ArrayList<>();
    report.add(coverageIssue);

    String issueReportMarkdown = printer.printReportMarkdown(report, project);
    String reportString = "## SonarQube analysis Overview\n"
        + "| Total New Issues | 1 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 0 |\n"
        + "| CRITICAL | 0 |\n"
        + "| MAJOR | 1 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|------------|\n\n\n"
        + "| Coverage |\n"
        + "|---------------|\n"
        + "| *MAJOR* - some text [[coverageEvolution-java:bla](sonarqube/URL/coding_rules#rule_key=coverageEvolution-java:bla)] |"
        + "\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithEmptyCoverageReport() {
    report.remove(coverageIssue);

    String issueReportMarkdown = printer.printReportMarkdown(report, project);
    String reportString = "## SonarQube analysis Overview\n"
        + "| Total New Issues | 3 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 1 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n\n\n";

    assertEquals(reportString, issueReportMarkdown);
  }
}
