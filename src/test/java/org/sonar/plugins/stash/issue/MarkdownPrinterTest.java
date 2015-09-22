package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class MarkdownPrinterTest {

  SonarQubeIssuesReport issueReport = new SonarQubeIssuesReport();
  
  @Before
  public void setUp(){
    SonarQubeIssue issueBlocker = new SonarQubeIssue("key1", "BLOCKER", "messageBlocker", "RuleBlocker", "pathBlocker", 1);
    SonarQubeIssue issueCritical = new SonarQubeIssue("key2", "CRITICAL", "messageCritical", "RuleCritical", "pathCritical", 1);
    SonarQubeIssue issueMajor = new SonarQubeIssue("key3", "MAJOR", "messageMajor", "RuleMajor", "pathMajor", 1);
    
    issueReport.add(issueBlocker);
    issueReport.add(issueCritical);
    issueReport.add(issueMajor);
  }
  
  @Test
  public void testPrintIssueMarkdown() {
    String sonarQubeURL = "sonarqube/URL";
    SonarQubeIssue issueBlocker = new SonarQubeIssue("key1", "BLOCKER", "messageBlocker", "RuleBlocker", "pathBlocker", 1);
    
    String issueMarkdown = MarkdownPrinter.printIssueMarkdown(issueBlocker, sonarQubeURL);
    assertTrue(StringUtils.equals(issueMarkdown, "*BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)]"));
  }

  @Test
  public void testPrintIssueNumberBySeverityMarkdown() {
    String issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| BLOCKER | 1 |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueNumberBySeverityMarkdown(issueReport, "INFO");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| INFO | 0 |\n"));
  }

  @Test
  public void testPrintIssueListBySeverityMarkdown() {
    String sonarQubeURL = "sonarqube/URL";
    
    String issueReportMarkdown = MarkdownPrinter.printIssueListBySeverityMarkdown(issueReport, sonarQubeURL, "BLOCKER");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| *BLOCKER* - messageBlocker [[RuleBlocker](sonarqube/URL/coding_rules#rule_key=RuleBlocker)] |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueListBySeverityMarkdown(issueReport, sonarQubeURL, "CRITICAL");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| *CRITICAL* - messageCritical [[RuleCritical](sonarqube/URL/coding_rules#rule_key=RuleCritical)] |\n"));
    
    issueReportMarkdown = MarkdownPrinter.printIssueListBySeverityMarkdown(issueReport, sonarQubeURL, "MAJOR");
    assertTrue(StringUtils.equals(issueReportMarkdown, "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n"));
  }

  @Test
  public void testPrintReportMarkdown() {
    String sonarQubeURL = "sonarqube/URL";
    int issueThreshold = 100;
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(issueReport, sonarQubeURL, issueThreshold);
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
        + "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n";
  
    assertTrue(StringUtils.equals(issueReportMarkdown, reportString));
  }
  
  @Test
  public void testPrintReportMarkdownWithIssueLimitation() {
    String sonarQubeURL = "sonarqube/URL";
    int issueThreshold = 2;
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(issueReport, sonarQubeURL, issueThreshold);
    String reportString = "## SonarQube analysis Overview\n"
        + "### Too many issues detected (3/2): Issues cannot be displayed in Diff view.\n\n"
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
        + "| *MAJOR* - messageMajor [[RuleMajor](sonarqube/URL/coding_rules#rule_key=RuleMajor)] |\n";
  
    assertTrue(StringUtils.equals(issueReportMarkdown, reportString));
  }
  
  @Test
  public void testPrintEmptyReportMarkdown() {
    String sonarQubeURL = "sonarqube/URL";
    int issueThreshold = 100;
    SonarQubeIssuesReport issueReport = new SonarQubeIssuesReport();
    
    String issueReportMarkdown = MarkdownPrinter.printReportMarkdown(issueReport, sonarQubeURL, issueThreshold);
    String reportString = "## SonarQube analysis Overview\n"
        + "### No new issues detected!";
        
    assertTrue(StringUtils.equals(issueReportMarkdown, reportString));
  }

}
