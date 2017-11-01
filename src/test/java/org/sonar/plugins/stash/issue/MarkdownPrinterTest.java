package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.DefaultIssue;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

public class MarkdownPrinterTest {

  PostJobIssue issue;

  List<PostJobIssue> report = new ArrayList<>();

  private static final String SONAR_URL = "sonarqube/URL";
  private static final String STASH_URL = "stash/URL";
  private MarkdownPrinter printer;
  private int issueThreshold;
  private DummyIssuePathResolver ipr;

  PullRequestRef pr = PullRequestRef.builder()
      .setProject("stashProject")
      .setRepository("stashRepo")
      .setPullRequestId(1)
      .build();

  @Before
  public void setUp() {
    PostJobIssue issueBlocker = new DefaultIssue().setKey("key1")
        .setSeverity(Severity.BLOCKER)
        .setMessage("messageBlocker")
        .setRuleKey(RuleKey.of("RepoBlocker", "RuleBlocker"))
        .setInputComponent(new DefaultInputFile("foo1", "bar1"))
        .setLine(1);
    PostJobIssue issueCritical = new DefaultIssue().setKey("key2")
        .setSeverity(Severity.CRITICAL)
        .setMessage("messageCritical")
        .setRuleKey(RuleKey.of("RepoCritical", "RuleCritical"))
        .setInputComponent(new DefaultInputFile("foo2", "bar2"))
        .setLine(1);
    PostJobIssue issueMajor = new DefaultIssue().setKey("key3")
        .setSeverity(Severity.MAJOR)
        .setMessage("messageMajor")
        .setRuleKey(RuleKey.of("RepoMajor", "RuleMajor"))
        .setInputComponent(new DefaultInputFile("foo3", "bar3"))
        .setLine(1);

    report.add(issueBlocker);
    report.add(issueCritical);
    report.add(issueMajor);

    issue = issueBlocker;

    issueThreshold = 100;
    ipr = new DummyIssuePathResolver();

    printer = new MarkdownPrinter(ipr, STASH_URL, pr, issueThreshold, SONAR_URL);
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
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.BLOCKER)
    );

    assertEquals(
        "| MAJOR | 1 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.MAJOR)
    );

    assertEquals(
        "| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.INFO)
    );
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoIssues() {
    Collection<PostJobIssue> report = new ArrayList<>();

    assertEquals("| BLOCKER | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.BLOCKER));
    assertEquals("| CRITICAL | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.CRITICAL));
    assertEquals("| MAJOR | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.MAJOR));
    assertEquals("| MINOR | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.MINOR));
    assertEquals("| INFO | 0 |\n",
        MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, Severity.INFO));
  }

  @Test
  public void testPrintReportMarkdown() {
    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "| Total New Issues | 3 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 1 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|-------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |"
        + "\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithIssueLimitation() {
    printer = new MarkdownPrinter(ipr, STASH_URL, pr, 3, SONAR_URL);
    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "### Too many issues detected (3/3): Issues cannot be displayed in Diff view.\n\n"
        + "| Total New Issues | 3 |\n"
        + "|-----------------|------|\n"
        + "| BLOCKER | 1 |\n"
        + "| CRITICAL | 1 |\n"
        + "| MAJOR | 1 |\n"
        + "| MINOR | 0 |\n"
        + "| INFO | 0 |\n\n\n"
        + "| Issues list |\n"
        + "|-------------|\n"
        + "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |"
        + "\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintEmptyReportMarkdown() {
    report = new ArrayList<>();

    String issueReportMarkdown = printer.printReportMarkdown(report);
    String reportString = "## SonarQube analysis Overview\n"
        + "### No new issues detected!\n\n";

    assertEquals(reportString, issueReportMarkdown);
  }
}
