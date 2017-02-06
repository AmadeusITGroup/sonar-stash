package org.sonar.plugins.stash.issue;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.PullRequestRef;

import java.util.Map;

public final class MarkdownPrinter {

  static final String NEW_LINE = "\n";
  static final String CODING_RULES_RULE_KEY = "coding_rules#rule_key=";
  
  private MarkdownPrinter(){
    // DO NOTHING
  }
    
  public static String printCoverageIssueMarkdown(String stashProject, String stashRepo, String pullRequestId, String stashURL, CoverageIssue issue) {
    StringBuilder sb = new StringBuilder();
    sb.append(printSeverityMarkdown(issue.getSeverity())).append(issue.getMessage()).append(" [[").append("file").append("]")
        .append("(").append(stashURL).append("/projects/").append(stashProject).append("/repos/").append(stashRepo)
        .append("/pull-requests/").append(pullRequestId).append("/diff#").append(issue.getPath()).append(")]");

    return sb.toString();
  }
  
  public static String printSeverityMarkdown(String severity) {
    StringBuilder sb = new StringBuilder();
    sb.append("*").append(StringUtils.upperCase(severity)).append("*").append(" - ");

    return sb.toString();
  }

  public static String printIssueNumberBySeverityMarkdown(SonarQubeIssuesReport report, CoverageIssuesReport coverageReport, String severity) {
    StringBuilder sb = new StringBuilder();
    int issueNumber = report.countIssues(severity) + coverageReport.countLoweredIssues(severity);
    sb.append("| ").append(severity).append(" | ").append(issueNumber).append(" |").append(NEW_LINE);

    return sb.toString();
  }

  public static String printIssueListBySeverityMarkdown(SonarQubeIssuesReport report, String sonarQubeURL, String severity) {
    StringBuilder sb = new StringBuilder();

    Map<String, SonarQubeIssue> rules = report.getUniqueRulesBySeverity(severity);

    // applying squid:S2864 optimization
    for (Map.Entry<String, SonarQubeIssue> rule : rules.entrySet()) {
      SonarQubeIssue issue = rule.getValue();
      sb.append("| ").append(issue.printIssueMarkdown(sonarQubeURL)).append(" |").append(NEW_LINE);
    }

    return sb.toString();
  }
  
  public static String printReportMarkdown(PullRequestRef pr, String stashURL, String sonarQubeURL, SonarQubeIssuesReport report,
                                           CoverageIssuesReport coverageReport, int issueThreshold) {
    
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

    String stashProject  = pr.project();
    String stashRepo     = pr.repository();
    int pullRequestId = pr.pullRequestId();

    if ((report.getIssues() == null) || (report.getIssues().isEmpty() && coverageReport.getLoweredIssues().isEmpty())) {
    
      sb.append("### No new issues detected!");
      sb.append(NEW_LINE).append(NEW_LINE);
    
    } else {
      
      // Number of issue per severity
      int issueNumber = report.countIssues() + coverageReport.countLoweredIssues();
      
      if (issueNumber >= issueThreshold) {
        sb.append("### Too many issues detected ");
        sb.append("(").append(issueNumber).append("/").append(issueThreshold).append(")");
        sb.append(": Issues cannot be displayed in Diff view.").append(NEW_LINE).append(NEW_LINE);
      }
      
      sb.append("| Total New Issues | ").append(issueNumber).append(" |").append(NEW_LINE);
      sb.append("|-----------------|------|").append(NEW_LINE);
      sb.append(printIssueNumberBySeverityMarkdown(report, coverageReport, Severity.BLOCKER));
      sb.append(printIssueNumberBySeverityMarkdown(report, coverageReport, Severity.CRITICAL));
      sb.append(printIssueNumberBySeverityMarkdown(report, coverageReport, Severity.MAJOR));
      sb.append(printIssueNumberBySeverityMarkdown(report, coverageReport, Severity.MINOR));
      sb.append(printIssueNumberBySeverityMarkdown(report, coverageReport, Severity.INFO));
      sb.append(NEW_LINE).append(NEW_LINE);
  
      // Issue list
      sb.append("| Issues list |").append(NEW_LINE);
      sb.append("|------------|").append(NEW_LINE);
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.BLOCKER));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.CRITICAL));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.MAJOR));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.MINOR));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.INFO));
      sb.append(NEW_LINE).append(NEW_LINE);
    }
    
    // Code coverage
    if (! coverageReport.isEmpty()) {
      sb.append(printCoverageReportMarkdown(stashProject, stashRepo, pullRequestId, coverageReport, stashURL));
    }

    return sb.toString();
  }
  
  public static String printCoverageReportMarkdown(String stashProject, String stashRepo, int pullRequestId, CoverageIssuesReport coverageReport, String stashURL) {
    StringBuilder sb = new StringBuilder("| Code Coverage: ");

    double projectCoverage = coverageReport.getProjectCoverage();
    double diffProjectCoverage = coverageReport.getEvolution();
    
    sb.append(projectCoverage).append("% (").append((diffProjectCoverage > 0)? "+" : "").append(diffProjectCoverage).append("%)").append(" |").append(NEW_LINE);
    sb.append("|---------------|").append(NEW_LINE);
    
    for (Issue issue : coverageReport.getIssues()) {
        CoverageIssue coverageIssue = (CoverageIssue) issue;
        
        if (coverageIssue.isLowered()) {
          sb.append("| ").append(MarkdownPrinter.printCoverageIssueMarkdown(stashProject, stashRepo, String.valueOf(pullRequestId), stashURL, coverageIssue)).append(" |").append(NEW_LINE);
        }
    }
    
    return sb.toString();
  }

}
