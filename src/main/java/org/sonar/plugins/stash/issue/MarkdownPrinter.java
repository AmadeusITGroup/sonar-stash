package org.sonar.plugins.stash.issue;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.IssuePathResolver;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.coverage.CoverageRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;
import static org.sonar.plugins.stash.StashPluginUtils.formatPercentage;
import static org.sonar.plugins.stash.StashPluginUtils.getUniqueRulesBySeverity;

public final class MarkdownPrinter {

  static final String NEW_LINE = "\n";
  static final String CODING_RULES_RULE_KEY = "coding_rules#rule_key=";
  
  private MarkdownPrinter(){
    // DO NOTHING
  }
    
  public static String printCoverageIssueMarkdown(String stashProject, String stashRepo, String pullRequestId, String stashURL, Issue issue, IssuePathResolver issuePathResolver) {
    StringBuilder sb = new StringBuilder();
    sb.append(printSeverityMarkdown(issue.severity())).append(issue.message()).append(" [[").append("file").append("]")
        .append("(").append(stashURL).append("/projects/").append(stashProject).append("/repos/").append(stashRepo)
        .append("/pull-requests/").append(pullRequestId).append("/diff#").append(issuePathResolver.getIssuePath(issue)).append(")]");

    return sb.toString();
  }
  
  public static String printSeverityMarkdown(String severity) {
    StringBuilder sb = new StringBuilder();
    sb.append("*").append(StringUtils.upperCase(severity)).append("*").append(" - ");

    return sb.toString();
  }

  public static String printIssueNumberBySeverityMarkdown(List<Issue> report, String severity) {
    StringBuilder sb = new StringBuilder();
    long issueNumber = countIssuesBySeverity(report, severity);
    sb.append("| ").append(severity).append(" | ").append(issueNumber).append(" |").append(NEW_LINE);

    return sb.toString();
  }

  public static String printIssueListBySeverityMarkdown(List<Issue> report, String sonarQubeURL, String severity) {
    StringBuilder sb = new StringBuilder();

    Map<String, Issue> rules = getUniqueRulesBySeverity(report, severity);

    // applying squid:S2864 optimization
    for (Map.Entry<String, Issue> rule : rules.entrySet()) {
      Issue issue = rule.getValue();
      sb.append("| ").append(printIssueMarkdown(issue, sonarQubeURL)).append(" |").append(NEW_LINE);
    }

    return sb.toString();
  }

  public static String printIssueMarkdown(Issue issue, String sonarQubeURL) {
    StringBuilder sb = new StringBuilder();
    sb.append(MarkdownPrinter.printSeverityMarkdown(issue.severity())).append(issue.message()).append(" [[").append(issue.ruleKey())
            .append("]").append("(").append(sonarQubeURL).append("/").append(MarkdownPrinter.CODING_RULES_RULE_KEY).append(issue.ruleKey()).append(")]");

    return sb.toString();
  }

  public static String printReportMarkdown(PullRequestRef pr, String stashURL, String sonarQubeURL, List<Issue> report,
                                           int issueThreshold, Double projectCoverage, Double previousProjectCoverage, IssuePathResolver issuePathResolver) {
    
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

    String stashProject  = pr.project();
    String stashRepo     = pr.repository();
    int pullRequestId = pr.pullRequestId();

    List<Issue> coverageIssues = new ArrayList<>();
    List<Issue> generalIssues = new ArrayList<>();
    for (Issue issue : report) {
      if (CoverageRule.isDecreasingLineCoverage(issue.key())) {
        coverageIssues.add(issue);
      } else {
        generalIssues.add(issue);
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
      sb.append(printCoverageReportMarkdown(stashProject, stashRepo, pullRequestId, coverageIssues, stashURL, projectCoverage, previousProjectCoverage, issuePathResolver));
    }

    return sb.toString();
  }
  
  public static String printCoverageReportMarkdown(String stashProject, String stashRepo, int pullRequestId, List<Issue> coverageReport, String stashURL,
                                                   Double projectCoverage, Double previousProjectCoverage, IssuePathResolver issuePathResolver) {
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
      sb.append("| ").append(MarkdownPrinter.printCoverageIssueMarkdown(stashProject, stashRepo, String.valueOf(pullRequestId), stashURL, issue, issuePathResolver)).append(" |").append(NEW_LINE);
    }
    
    return sb.toString();
  }

}
