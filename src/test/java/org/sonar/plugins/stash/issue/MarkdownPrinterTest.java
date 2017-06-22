package org.sonar.plugins.stash.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.SonarSettings;
import org.sonar.plugins.stash.coverage.CoverageRule;
import org.sonar.plugins.stash.coverage.CoverageSensorTest;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
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
                                      .setRuleKey(CoverageRule.decreasingLineCoverageRule("java"))
                                      .setMessage(CoverageSensorTest.formatIssueMessage("path/code/coverage",
                                                                                        40.0,
                                                                                        50.0));

    report.add(coverageIssue);

    issuePathResolver = new DummyIssuePathResolver();
    issuePathResolver.add(coverageIssue, "path/code/coverage");
  }

  @Test
  public void testPrintIssueMarkdown() {
    assertEquals(
        "*BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)]",
        MarkdownPrinter.printIssueMarkdown(report.get(0), SONAR_URL)
    );
  }

  @Test
  public void testPrintCoverageIssueMarkdown() {
    issuePathResolver.add(coverageIssue, "path/code/coverage");
    PullRequestRef prr = PullRequestRef.builder()
                                       .setProject("project")
                                       .setRepository("repo")
                                       .setPullRequestId(1)
                                       .build();
    String coverageMarkdown = MarkdownPrinter.printCoverageIssueMarkdown(prr,
                                                                         STASH_URL,
                                                                         coverageIssue,
                                                                         issuePathResolver);
    assertEquals(
        "*MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)]",
        coverageMarkdown);
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

    assertEquals("| BLOCKER | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER"));
    assertEquals("| CRITICAL | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "CRITICAL"));
    assertEquals("| MAJOR | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR"));
    assertEquals("| MINOR | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MINOR"));
    assertEquals("| INFO | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoSonarQubeIssues() {
    List<Issue> report = new ArrayList<>();
    report.add(coverageIssue);

    assertEquals("| BLOCKER | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER"));
    assertEquals("| MAJOR | 1 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR"));
    assertEquals("| INFO | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoCoverageIssues() {
    report.remove(coverageIssue);

    assertEquals("| BLOCKER | 1 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "BLOCKER"));
    assertEquals("| MAJOR | 1 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "MAJOR"));
    assertEquals("| INFO | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report, "INFO"));
  }

  @Test
  public void testPrintIssueListBySeverityMarkdown() {
    report.remove(coverageIssue);

    assertEquals(
        "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n",
        MarkdownPrinter.printIssueListBySeverityMarkdown(report, SONAR_URL, "BLOCKER"));

    assertEquals(
        "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n",
        MarkdownPrinter.printIssueListBySeverityMarkdown(report, SONAR_URL, "CRITICAL"));

    assertEquals(
        "| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n",
        MarkdownPrinter.printIssueListBySeverityMarkdown(report, SONAR_URL, "MAJOR"));
  }

  private String printReportMarkdown(List<Issue> report, int issueThreshold) {
    SonarSettings sonarSet = new SonarSettings(SONAR_URL, issueThreshold, 40.0, 50.0);
    return MarkdownPrinter.printReportMarkdown(pr, STASH_URL, sonarSet, report, issuePathResolver);
  }

  @Test
  public void testPrintReportMarkdown() {
    String issueReportMarkdown = printReportMarkdown(report, 100);
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
                          + "| Line Coverage: 40.0% (-10.0%) |\n"
                          + "|---------------|\n"
                          + "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/stashProject/repos/stashRepo/pull-requests/1/diff#path/code/coverage)] |\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithIssueLimitation() {
    String issueReportMarkdown = printReportMarkdown(report, 3);
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
                          + "| Line Coverage: 40.0% (-10.0%) |\n"
                          + "|---------------|\n"
                          + "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/stashProject/repos/stashRepo/pull-requests/1/diff#path/code/coverage)] |\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintEmptyReportMarkdown() {
    report = new ArrayList<>();

    String issueReportMarkdown = printReportMarkdown(report, 100);
    String reportString = "## SonarQube analysis Overview\n"
                          + "### No new issues detected!\n\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithEmptySonarQubeReportAndWithLoweredIssues() {
    report = new ArrayList<>();
    report.add(coverageIssue);

    String issueReportMarkdown = printReportMarkdown(report, 100);
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
                          + "| Line Coverage: 40.0% (-10.0%) |\n"
                          + "|---------------|\n"
                          + "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/stashProject/repos/stashRepo/pull-requests/1/diff#path/code/coverage)] |\n";

    assertEquals(reportString, issueReportMarkdown);
  }

  @Test
  public void testPrintReportMarkdownWithEmptyCoverageReport() {
    report.remove(coverageIssue);

    String issueReportMarkdown = printReportMarkdown(report, 100);
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

  private String printCoverageReportMarkdown(List<Issue> report,
                                             Double projectCoverage,
                                             Double previousProjectCoverage) {
    PullRequestRef prr = PullRequestRef.builder()
                                       .setProject("project")
                                       .setRepository("repo")
                                       .setPullRequestId(1)
                                       .build();
    SonarSettings sonarSet = new SonarSettings(SONAR_URL, 100, projectCoverage, previousProjectCoverage);
    return MarkdownPrinter.printCoverageReportMarkdown(prr, report, STASH_URL, sonarSet, issuePathResolver);
  }

  private String printCoverageReportMarkdown(Double projectCoverage, Double previousProjectCoverage) {
    return printCoverageReportMarkdown(new ArrayList<>(), projectCoverage, previousProjectCoverage);
  }

  @Test
  public void testPrintCoverageReportMarkdown() {
    List<Issue> report = new ArrayList<>();
    report.add(coverageIssue);

    String reportMarkdown = printCoverageReportMarkdown(report, 40.0, 50.0);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (-10.0%) |\n" +
                                    "|---------------|\n" +
                                    "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)] |\n";

    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithPositiveCoverage() {
    String reportMarkdown = printCoverageReportMarkdown(50.0, 40.0);
    String expectedReportMarkdown = "| Line Coverage: 50.0% (+10.0%) |\n" +
                                    "|---------------|\n";

    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithNoEvolution() {
    String reportMarkdown = printCoverageReportMarkdown(40.0, 40.0);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (0.0%) |\n" +
                                    "|---------------|\n";

    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithNoIssues() {
    String reportMarkdown = printCoverageReportMarkdown(0.0, 40.0);
    String expectedReportMarkdown = "| Line Coverage: 0.0% (-40.0%) |\n" +
                                    "|---------------|\n";

    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithNoLoweredIssues() {
    String reportMarkdown = printCoverageReportMarkdown(40.0, 30.0);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (+10.0%) |\n" +
                                    "|---------------|\n";

    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithNoCurrentAndPreviousCoverage() {
    assertEquals(
        "| Line Coverage: |\n|---------------|\n",
        printCoverageReportMarkdown(null, null)
    );
  }

  @Test
  public void testPrintCoverageReportMarkdownWithNoCurrentCoverage() {
    assertEquals(
        "| Line Coverage: |\n|---------------|\n",
        printCoverageReportMarkdown(null, 30.0)
    );
  }

  @Test
  public void testPrintCoverageReportMarkdownWithNoPreviousCoverage() {
    assertEquals(
        "| Line Coverage: 40.0% |\n|---------------|\n",
        printCoverageReportMarkdown(40.0, null)
    );
  }

  @Test
  public void testConstructorIsPrivate() throws Exception {

    // Let's use this for the greater good: we make sure that nobody can create an instance of this class
    Constructor constructor = MarkdownPrinter.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));

    // This part is for code coverage only (but is re-using the elments above... -_^)
    constructor.setAccessible(true);
    constructor.newInstance();
  }
}
