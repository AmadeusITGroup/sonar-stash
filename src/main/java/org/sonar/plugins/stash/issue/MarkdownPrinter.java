package org.sonar.plugins.stash.issue;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.coverage.CoverageRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sonar.plugins.stash.StashPluginUtils.formatPercentage;

public final class MarkdownPrinter {

  static final String NEW_LINE = "\n";
  static final String CODING_RULES_RULE_KEY = "coding_rules#rule_key=";
  
  private MarkdownPrinter(){
    // DO NOTHING
  }
    
  public static String printCoverageIssueMarkdown(String stashProject, String stashRepo, String pullRequestId, String stashURL, Issue issue) {
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

  public static String printIssueNumberBySeverityMarkdown(SonarQubeIssuesReport report, String severity) {
    StringBuilder sb = new StringBuilder();
    int issueNumber = report.countIssues(severity);
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
                                           int issueThreshold, Double projectCoverage, Double previousProjectCoverage) {
    
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

    String stashProject  = pr.project();
    String stashRepo     = pr.repository();
    int pullRequestId = pr.pullRequestId();

    List<Issue> coverageIssues = new ArrayList<>();
    List<Issue> generalIssues = new ArrayList<>();

    if (report.getIssues() != null ) {
      for (Issue issue : report.getIssues()) {
        if (CoverageRule.isDecreasingLineCoverage(issue.getKey())) {
          coverageIssues.add(issue);
        } else {
          generalIssues.add(issue);
        }
      }
    }

    if (generalIssues.isEmpty() && coverageIssues.isEmpty()) {
    
      sb.append("### No new issues detected!");
      sb.append(NEW_LINE).append(NEW_LINE);
    
    } else {
      
      // Number of issue per severity
      int issueNumber = generalIssues.size() + coverageIssues.size();
      
      if (issueNumber >= issueThreshold) {
        sb.append("### Too many issues detected ");
        sb.append("(").append(issueNumber).append("/").append(issueThreshold).append(")");
        sb.append(": Issues cannot be displayed in Diff view.").append(NEW_LINE).append(NEW_LINE);
      }
      
      sb.append("| Total New Issues | ").append(issueNumber).append(" |").append(NEW_LINE);
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
      // FIXME do not pass the whole report here, just general issues
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.BLOCKER));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.CRITICAL));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.MAJOR));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.MINOR));
      sb.append(printIssueListBySeverityMarkdown(report, sonarQubeURL, Severity.INFO));
      sb.append(NEW_LINE).append(NEW_LINE);
    }
    
    // Code coverage
    if (!coverageIssues.isEmpty()) {
      sb.append(printCoverageReportMarkdown(stashProject, stashRepo, pullRequestId, coverageIssues, stashURL, projectCoverage, previousProjectCoverage));
    }

    return sb.toString();
  }
  
  public static String printCoverageReportMarkdown(String stashProject, String stashRepo, int pullRequestId, List<Issue> coverageReport, String stashURL,
                                                   Double projectCoverage, Double previousProjectCoverage) {
    StringBuilder sb = new StringBuilder("| Line Coverage: ");

    double diffProjectCoverage = projectCoverage - previousProjectCoverage;
    
    sb.append(formatPercentage(projectCoverage))
            .append("% (")
            .append((diffProjectCoverage > 0)? "+" : "")
            .append(formatPercentage(diffProjectCoverage))
            .append("%)")
            .append(" |")
            .append(NEW_LINE);
    sb.append("|---------------|").append(NEW_LINE);
    
    for (Issue issue : coverageReport) {
      sb.append("| ").append(MarkdownPrinter.printCoverageIssueMarkdown(stashProject, stashRepo, String.valueOf(pullRequestId), stashURL, issue)).append(" |").append(NEW_LINE);
    }
    
    return sb.toString();
  }

}
