package org.sonar.plugins.stash.issue;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.Severity;

import java.util.Map;

public final class MarkdownPrinter {

  private static final String NEW_LINE = "\n";
  private static final String CODING_RULES_RULE_KEY = "coding_rules#rule_key=";
  
  private MarkdownPrinter(){
    // DO NOTHING
  }
  
  public static String printIssueMarkdown(SonarQubeIssue issue, String sonarQubeURL) {
    StringBuilder sb = new StringBuilder();
    sb.append(printSeverityMarkdown(issue.getSeverity())).append(issue.getMessage()).append(" [[").append(issue.getRule())
        .append("]").append("(").append(sonarQubeURL).append("/").append(CODING_RULES_RULE_KEY).append(issue.getRule()).append(")]");

    return sb.toString();
  }
  
  public static String printSeverityMarkdown(String severity) {
    StringBuilder sb = new StringBuilder();
    sb.append("*").append(StringUtils.upperCase(severity)).append("*").append(" - ");

    return sb.toString();
  }

  public static String printIssueNumberBySeverityMarkdown(SonarQubeIssuesReport report, String severity) {
    StringBuilder sb = new StringBuilder();
    sb.append("| ").append(severity).append(" | ").append(report.countIssues(severity)).append(" |").append(NEW_LINE);

    return sb.toString();
  }

  public static String printIssueListBySeverityMarkdown(SonarQubeIssuesReport report, String sonarQubeURL, String severity) {
    StringBuilder sb = new StringBuilder();

    Map<String, SonarQubeIssue> rules = report.getUniqueRulesBySeverity(severity);
    for (SonarQubeIssue issue : rules.values()) {
      sb.append("| ").append(MarkdownPrinter.printIssueMarkdown(issue, sonarQubeURL)).append(" |").append(NEW_LINE);
    }

    return sb.toString();
  }

  /**
   * Get issue report.
   */
  public static String printReportMarkdown(SonarQubeIssuesReport report, String sonarQubeURL, int issueThreshold) {
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

    if ((report.getIssues() == null) || (report.getIssues().isEmpty())) {
      sb.append("### No new issues detected!");
    } else {
      
      if (report.countIssues() >= issueThreshold) {
        sb.append("### Too many issues detected ");
        sb.append("(").append(report.countIssues()).append("/").append(issueThreshold).append(")");
        sb.append(": Issues cannot be displayed in Diff view.").append(NEW_LINE).append(NEW_LINE);
      }
      
      // Number of issue per severity
      sb.append("| Total New Issues | ").append(report.countIssues()).append(" |").append(NEW_LINE);
      sb.append("|-----------------|------|").append(NEW_LINE);
      sb.append(printIssueNumberBySeverityMarkdown(report, Severity.BLOCKER));
      sb.append(printIssueNumberBySeverityMarkdown(report, Severity.CRITICAL));
      sb.append(printIssueNumberBySeverityMarkdown(report, Severity.MAJOR));
      sb.append(printIssueNumberBySeverityMarkdown(report, Severity.MINOR));
      sb.append(printIssueNumberBySeverityMarkdown(report, Severity.INFO));
      sb.append(NEW_LINE).append(NEW_LINE);

      // Issue list
      sb.append("| Issues list |").append(NEW_LINE);
      sb.append("|------------|").append(NEW_LINE);
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.BLOCKER));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.CRITICAL));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.MAJOR));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.MINOR));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.INFO));
      
    }

    return sb.toString();
  }

}
