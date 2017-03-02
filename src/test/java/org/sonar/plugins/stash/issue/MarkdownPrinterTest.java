package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.stash.PullRequestRef;

public class MarkdownPrinterTest {
  
  CoverageIssue coverageIssue;
  
  SonarQubeIssuesReport issueReport = new SonarQubeIssuesReport();
  
  CoverageIssuesReport coverageReport = new CoverageIssuesReport();
  
  private static final String SONAR_URL = "sonarqube/URL";
  private static final String STASH_URL = "stash/URL";
  
  PullRequestRef pr = PullRequestRef.builder()
          .setProject("stashProject")
          .setRepository("stashRepo")
          .setPullRequestId(1)
          .build();

  
  @Before
  public void setUp(){
    SonarQubeIssue issueBlocker = new SonarQubeIssue("key1", "BLOCKER", "messageBlocker", "RuleBlocker", "pathBlocker", 1);
    SonarQubeIssue issueCritical = new SonarQubeIssue("key2", "CRITICAL", "messageCritical", "RuleCritical", "pathCritical", 1);
    SonarQubeIssue issueMajor = new SonarQubeIssue("key3", "MAJOR", "messageMajor", "RuleMajor", "pathMajor", 1);
    
    issueReport.add(issueBlocker);
    issueReport.add(issueCritical);
    issueReport.add(issueMajor);
    
    coverageIssue = new CoverageIssue("MAJOR", "path/code/coverage");
    
    coverageIssue.setLinesToCover(100);
    coverageIssue.setUncoveredLines(60);
    coverageIssue.setPreviousCoverage(50.00);
    
    coverageReport.add(coverageIssue);
    coverageReport.setPreviousProjectCoverage(50.00);
  }
  
  @Test
  public void testPrintIssueMarkdown() {
    SonarQubeIssue issueBlocker = new SonarQubeIssue("key1", "BLOCKER", "messageBlocker", "RuleBlocker", "pathBlocker", 1);
    
    String issueMarkdown = issueBlocker.printIssueMarkdown(SONAR_URL);
    assertTrue(StringUtils.equals(issueMarkdown, "*BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)]"));
  }
  
  @Test
  public void testPrintCoverageIssueMarkdown() {
    String coverageMarkdown = MarkdownPrinter.printCoverageIssueMarkdown("project", "repo", "1", STASH_URL, coverageIssue);
    assertEquals("*MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)]",
                  coverageMarkdown);
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdown() {
    String issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| BLOCKER | 1 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "MAJOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| MAJOR | 2 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "INFO");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| INFO | 0 |\n"));
  }
  
  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoIssues() {
    SonarQubeIssuesReport issueReport = new SonarQubeIssuesReport();
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| BLOCKER | 0 |\n"));
  
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "CRITICAL");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| CRITICAL | 0 |\n"));
  
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "MAJOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| MAJOR | 0 |\n"));
  
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "MINOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| MINOR | 0 |\n"));
 
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "INFO");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| INFO | 0 |\n"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoSonarQubeIssues() {
    SonarQubeIssuesReport issueReport = new SonarQubeIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| BLOCKER | 0 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "MAJOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| MAJOR | 1 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "INFO");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| INFO | 0 |\n"));
  }
  
  @Test
  public void testPrintIssueNumberBySeverityMarkdownWithNoCoverageIssues() {
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| BLOCKER | 1 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "MAJOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| MAJOR | 1 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, coverageReport, "INFO");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| INFO | 0 |\n"));
  }
  
  @Test
  public void testPrintIssueListBySeverityMarkdown() {
    String issueReportMarkdown = MarkdownPrinter.printIssueListBySeverityMarkdown(issueReport, SONAR_URL, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| *BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)] |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueListBySeverityMarkdown(issueReport, SONAR_URL, "CRITICAL");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| *CRITICAL* - messageCritical [[RuleCritical](sonarqube/URL/coding_rules#rule_key=RuleCritical)] |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueListBySeverityMarkdown(issueReport, SONAR_URL, "MAJOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n"));
  }

  @Test
  public void testPrintReportMarkdown() {
    int issueThreshold = 100;
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(pr, STASH_URL, SONAR_URL, issueReport, coverageReport, issueThreshold);
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
        + "| *BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RuleCritical](sonarqube/URL/coding_rules#rule_key=RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n\n\n"
        + "| Line Coverage: 40.0% (-10.0%) |\n"
        + "|---------------|\n"
        + "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/stashProject/repos/stashRepo/pull-requests/1/diff#path/code/coverage)] |\n";
        
    assertEquals(reportString, issueReportMarkdown);
  }
  
  @Test
  public void testPrintReportMarkdownWithIssueLimitation() {
    int issueThreshold = 3;
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(pr, STASH_URL, SONAR_URL, issueReport, coverageReport, issueThreshold);
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
        + "| *BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RuleCritical](sonarqube/URL/coding_rules#rule_key=RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n\n\n"
        + "| Line Coverage: 40.0% (-10.0%) |\n"
        + "|---------------|\n"
        + "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/stashProject/repos/stashRepo/pull-requests/1/diff#path/code/coverage)] |\n";
  
    assertEquals(reportString, issueReportMarkdown);
  }
  
  @Test
  public void testPrintEmptyReportMarkdown() {
    int issueThreshold = 100;
    
    SonarQubeIssuesReport issueReport = new SonarQubeIssuesReport();
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(pr, STASH_URL, SONAR_URL, issueReport, coverageReport, issueThreshold);
    String reportString = "## SonarQube analysis Overview\n"
        + "### No new issues detected!\n\n";
        
    assertEquals(reportString, issueReportMarkdown);
  }
  
  @Test
  public void testPrintReportMarkdownWithEmptySonarQubeReportAndWithLoweredIssues() {
    issueReport = new SonarQubeIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(pr, STASH_URL, SONAR_URL, issueReport, coverageReport, 100);
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
    coverageReport = new CoverageIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(pr, STASH_URL, SONAR_URL, issueReport, coverageReport, 100);
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
        + "| *BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)] |\n"
        + "| *CRITICAL* - messageCritical [[RuleCritical](sonarqube/URL/coding_rules#rule_key=RuleCritical)] |\n"
        + "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n\n\n";
        
    assertEquals(reportString, issueReportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdown() {
    CoverageIssue coverageIssue = new CoverageIssue("MAJOR", "path/code/coverage");
    
    coverageIssue.setLinesToCover(100);
    coverageIssue.setUncoveredLines(60);
    coverageIssue.setPreviousCoverage(50.00);
    
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    coverageReport.add(coverageIssue);
    coverageReport.setPreviousProjectCoverage(50.00);
    
    String reportMarkdown = MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, coverageReport, STASH_URL);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (-10.0%) |\n" +
                                    "|---------------|\n" +
                                    "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.0% to 40.0%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)] |\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithRoundedPercentage() {
    CoverageIssue coverageIssue = new CoverageIssue("MAJOR", "path/code/coverage");

    coverageIssue.setLinesToCover(65);
    coverageIssue.setUncoveredLines(60);
    coverageIssue.setPreviousCoverage(50.25);

    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    coverageReport.add(coverageIssue);
    coverageReport.setPreviousProjectCoverage(50.25);

    String reportMarkdown = MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, coverageReport, STASH_URL);
    String expectedReportMarkdown = "| Line Coverage: 7.7% (-42.6%) |\n" +
            "|---------------|\n" +
            "| *MAJOR* - Line coverage of file path/code/coverage lowered from 50.3% to 7.7%. [[file](stash/URL/projects/project/repos/repo/pull-requests/1/diff#path/code/coverage)] |\n";

    assertEquals(expectedReportMarkdown, reportMarkdown);
  }

  @Test
  public void testPrintCoverageReportMarkdownWithPositiveCoverage() {
    CoverageIssue coverageIssue = new CoverageIssue("MAJOR", "path/code/coverage");
    
    coverageIssue.setLinesToCover(100);
    coverageIssue.setUncoveredLines(50);
    coverageIssue.setPreviousCoverage(40.00);
    
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    coverageReport.setPreviousProjectCoverage(40.0);
    coverageReport.add(coverageIssue);
    
    String reportMarkdown = MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, coverageReport, STASH_URL);
    String expectedReportMarkdown = "| Line Coverage: 50.0% (+10.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdownWithNoEvolution() {
    CoverageIssue coverageIssue = new CoverageIssue("MAJOR", "path/code/coverage");
    
    coverageIssue.setLinesToCover(100);
    coverageIssue.setUncoveredLines(60);
    coverageIssue.setPreviousCoverage(40.00);
    
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    coverageReport.setPreviousProjectCoverage(40.0);
    coverageReport.add(coverageIssue);
    
    String reportMarkdown = MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, coverageReport, STASH_URL);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (0.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdownWithNoIssues() {
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    coverageReport.setPreviousProjectCoverage(40.0);
        
    String reportMarkdown = MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, coverageReport, STASH_URL);
    String expectedReportMarkdown = "| Line Coverage: 0.0% (-40.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  @Test
  public void testPrintCoverageReportMarkdownWithNoLoweredIssues() {
    CoverageIssue coverageIssue = new CoverageIssue("MAJOR", "path/code/coverage");
    
    coverageIssue.setLinesToCover(100);
    coverageIssue.setUncoveredLines(60);
    coverageIssue.setPreviousCoverage(30.00);
    
    CoverageIssuesReport coverageReport = new CoverageIssuesReport();
    coverageReport.setPreviousProjectCoverage(30.0);
    coverageReport.add(coverageIssue);
        
    String reportMarkdown = MarkdownPrinter.printCoverageReportMarkdown("project", "repo", 1, coverageReport, STASH_URL);
    String expectedReportMarkdown = "| Line Coverage: 40.0% (+10.0%) |\n" +
                                    "|---------------|\n";
    
    assertEquals(expectedReportMarkdown, reportMarkdown);
  }
  
  

}
