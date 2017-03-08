package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.coverage.CoverageRule;
import org.sonar.plugins.stash.coverage.CoverageSensorTest;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

import java.util.ArrayList;
import java.util.List;

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
  public void setUp(){
    Issue issueBlocker = new DefaultIssue().setKey("key1").setSeverity(Severity.BLOCKER).setMessage("messageBlocker").setRuleKey(RuleKey.of("RepoBlocker", "RuleBlocker")).setLine(1);
    Issue issueCritical = new DefaultIssue().setKey("key2").setSeverity(Severity.CRITICAL).setMessage("messageCritical").setRuleKey(RuleKey.of("RepoCritical", "RuleCritical")).setLine(1);
    Issue issueMajor = new DefaultIssue().setKey("key3").setSeverity(Severity.MAJOR).setMessage("messageMajor").setRuleKey(RuleKey.of("RepoMajor", "RuleMajor")).setLine(1);

    report.add(issueBlocker);
    report.add(issueCritical);
    report.add(issueMajor);

    issue = issueBlocker;
    coverageIssue = new DefaultIssue().setKey("key4").setSeverity(Severity.MAJOR).setRuleKey(CoverageRule.decreasingLineCoverageRule("java"))
            .setMessage(CoverageSensorTest.formatIssueMessage("path/code/coverage", 40.0, 50.0));

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
    String coverageMarkdown = MarkdownPrinter.printCoverageIssueMarkdown("project", "repo", "1", STASH_URL, coverageIssue, issuePathResolver);
    assertEquals("*MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)]",
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

    assertEquals("| BLOCKER | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"BLOCKER"));
    assertEquals("| CRITICAL | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"CRITICAL"));
    assertEquals("| MAJOR | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"MAJOR"));
    assertEquals("| MINOR | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"MINOR"));
    assertEquals("| INFO | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"INFO"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoSonarQubeIssues() {
    List<Issue> report = new ArrayList<>();
    report.add(coverageIssue);

    assertEquals("| BLOCKER | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"BLOCKER"));
    assertEquals("| MAJOR | 1 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"MAJOR"));
    assertEquals("| INFO | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"INFO"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoCoverageIssues() {
    report.remove(coverageIssue);

    assertEquals("| BLOCKER | 1 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"BLOCKER"));
    assertEquals("| MAJOR | 1 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"MAJOR"));
    assertEquals("| INFO | 0 |\n", MarkdownPrinter.printIssueNumberBySeverityMarkdown(report,"INFO"));
  }
  
  @Test
  public void testPrintIssueListBySeverityMarkdown() {
    report.remove(coverageIssue);

    assertEquals( "| *BLOCKER* - messageBlocker [[RepoBlocker:RuleBlocker](sonarqube/URL/coding_rules#rule_key=RepoBlocker:RuleBlocker)] |\n",
            MarkdownPrinter.printIssueListBySeverityMarkdown(report, SONAR_URL, "BLOCKER"));

    assertEquals( "| *CRITICAL* - messageCritical [[RepoCritical:RuleCritical](sonarqube/URL/coding_rules#rule_key=RepoCritical:RuleCritical)] |\n",
            MarkdownPrinter.printIssueListBySeverityMarkdown(report, SONAR_URL, "CRITICAL"));

    assertEquals("| *MAJOR* - messageMajor [[RepoMajor:RuleMajor](sonarqube/URL/coding_rules#rule_key=RepoMajor:RuleMajor)] |\n",
            MarkdownPrinter.printIssueListBySeverityMarkdown(report, SONAR_URL, "MAJOR"));
  }

  private String printReportMarkdown(List<Issue> report, int issueThreshold) {
    return MarkdownPrinter.printReportMarkdown(pr, STASH_URL, SONAR_URL, report, issueThreshold, 40.0, 50.0, issuePathResolver);
  }

  @Test
  public void testPrintReportMarkdown() {
    int issueThreshold = 100;

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

  private String printCoverageReportMarkdown(List<Issue> report, double projectCoverage, double previousProjectCoverage) {
    return MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, report, STASH_URL, projectCoverage, previousProjectCoverage, issuePathResolver);
  }

  @Test
  public void testPrintCoverageReportMarkdown() {
    report = new ArrayList<>();
    report.add(coverageIssue);

    String reportMarkdown = printCoverageReportMarkdown(report, 40.0, 50.0);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (-10.0%) |\n" +
                                    "|---------------|\n" +
                                    "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)] |\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithPositiveCoverage() {
    report = new ArrayList<>();

    String reportMarkdown = printCoverageReportMarkdown(report, 50.0, 40.0);
    String expectedReportMarkdown = "| Line Coverage: 50.0% (+10.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdownWithNoEvolution() {
    report = new ArrayList<>();

    String reportMarkdown = printCoverageReportMarkdown(report, 40.0, 40.0);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (0.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdownWithNoIssues() {
    report = new ArrayList<>();

    String reportMarkdown = printCoverageReportMarkdown(report, 0.0, 40.0);
    String expectedReportMarkdown = "| Line Coverage: 0.0% (-40.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdownWithNoLoweredIssues() {
    report = new ArrayList<>();

    String reportMarkdown = printCoverageReportMarkdown(report, 40.0, 30.0);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (+10.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
}
