package org.sonar.plugins.stash.issue;

import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.IssuePathResolver;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.SonarSettings;
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
  static final List<String> orderedSeverities = Lists.reverse(Severity.ALL);
  
  private MarkdownPrinter(){
    // Hiding implicit public constructor with an explicit private one (squid:S1118)
  }
    
  public static String printCoverageIssueMarkdown(PullRequestRef prr,
                                                  String stashURL,
                                                  Issue issue,
                                                  IssuePathResolver issuePathResolver) {
    StringBuilder sb = new StringBuilder();
    sb.append(printSeverityMarkdown(issue.severity())).append(issue.message())
        .append(" [[").append("file").append("]").append("(").append(stashURL)
        .append("/projects/").append(prr.project())
        .append("/repos/").append(prr.repository())
        .append("/pull-requests/").append(String.valueOf(prr.pullRequestId()))
        .append("/diff#").append(issuePathResolver.getIssuePath(issue)).append(")]");

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
    sb.append(MarkdownPrinter.printSeverityMarkdown(issue.severity())).append(issue.message())
      .append(" [[").append(issue.ruleKey())
      .append("]").append("(").append(sonarQubeURL).append("/")
      .append(MarkdownPrinter.CODING_RULES_RULE_KEY).append(issue.ruleKey()).append(")]");

    return sb.toString();
  }

  public static String printReportMarkdown(PullRequestRef pr,
                                           String stashURL,
                                           SonarSettings sonarConf,
                                           List<Issue> allIssues,
                                           IssuePathResolver issuePathResolver) {
    
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

    List<Issue> coverageIssues = new ArrayList<>();
    List<Issue> generalIssues = new ArrayList<>();
    for (Issue issue : allIssues) {
      if (CoverageRule.isDecreasingLineCoverage(issue.ruleKey())) {
        coverageIssues.add(issue);
      } else {
        generalIssues.add(issue);
      }
    }

    if (generalIssues.isEmpty() && coverageIssues.isEmpty()) {
    
      sb.append("### No new issues detected!");
      sb.append(NEW_LINE).append(NEW_LINE);
    
    } else {
      
      int issueNumber = allIssues.size();
      
      if (issueNumber >= sonarConf.issueThreshold()) {
        sb.append("### Too many issues detected ");
        sb.append("(").append(issueNumber).append("/").append(sonarConf.issueThreshold()).append(")");
        sb.append(": Issues cannot be displayed in Diff view.").append(NEW_LINE).append(NEW_LINE);
      }
      
      sb.append("| Total New Issues | ").append(issueNumber).append(" |").append(NEW_LINE);
      sb.append("|-----------------|------|").append(NEW_LINE);
      for (String severity: orderedSeverities) {
        sb.append(printIssueNumberBySeverityMarkdown(allIssues, severity));
      }
      sb.append(NEW_LINE).append(NEW_LINE);
  
      // Issue list
      sb.append("| Issues list |").append(NEW_LINE);
      sb.append("|------------|").append(NEW_LINE);
      for (String severity: orderedSeverities) {
        sb.append(printIssueListBySeverityMarkdown(generalIssues, sonarConf.sonarQubeURL(), severity));
      }
      sb.append(NEW_LINE).append(NEW_LINE);
    }
    
    // Code coverage
    if (!coverageIssues.isEmpty()) {
      sb.append(printCoverageReportMarkdown(pr, coverageIssues, stashURL, sonarConf, issuePathResolver));
    }

    return sb.toString();
  }
  
  public static String printCoverageReportMarkdown(PullRequestRef prr,
                                                   List<Issue> coverageReport,
                                                   String stashURL,
                                                   SonarSettings sonarConf,
                                                   IssuePathResolver issuePathResolver) {
    StringBuilder sb = new StringBuilder("| Line Coverage: ");

    Double projectCoverage = sonarConf.projectCoverage();
    Double previousProjectCoverage = sonarConf.previousProjectCoverage();

    if (projectCoverage != null) {
      sb.append(formatPercentage(projectCoverage));
      sb.append("% ");

      if (previousProjectCoverage != null) {
        double diffProjectCoverage = projectCoverage - previousProjectCoverage;

        sb.append("(")
          .append((diffProjectCoverage > 0)? "+" : "")
          .append(formatPercentage(diffProjectCoverage))
          .append("%) ")
        ;

      }

    }

    sb.append("|");
    sb.append(NEW_LINE);
    sb.append("|---------------|").append(NEW_LINE);
    
    for (Issue issue : coverageReport) {
      sb.append("| ").append(MarkdownPrinter.printCoverageIssueMarkdown(prr, stashURL, issue, issuePathResolver))
                     .append(" |").append(NEW_LINE);
    }
    
    return sb.toString();
  }

}
