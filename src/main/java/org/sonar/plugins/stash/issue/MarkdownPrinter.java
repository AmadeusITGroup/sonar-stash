package org.sonar.plugins.stash.issue;

import com.google.common.collect.Lists;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.IssuePathResolver;
import org.sonar.plugins.stash.PullRequestRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.sonar.plugins.stash.CoverageCompat.isCoverageEvolution;
import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;
import static org.sonar.plugins.stash.StashPluginUtils.getUniqueRulesBySeverity;
import static org.sonar.plugins.stash.StashPluginUtils.isProjectWide;

public final class MarkdownPrinter {

  static final String NEW_LINE = "\n";
  static final String CODING_RULES_RULE_KEY = "coding_rules#rule_key=";
  static final List<String> orderedSeverities = Lists.reverse(Severity.ALL);

  private MarkdownPrinter() {
  }

  private String stashURL;
  private PullRequestRef pr;
  private IssuePathResolver issuePathResolver;
  private int issueThreshold;
  private String sonarQubeURL;

  public MarkdownPrinter(String stashURL, PullRequestRef pr, IssuePathResolver issuePathResolver,
      int issueThreshold, String sonarQubeURL) {
    this.stashURL = stashURL;
    this.pr = pr;
    this.issuePathResolver = issuePathResolver;
    this.issueThreshold = issueThreshold;
    this.sonarQubeURL = sonarQubeURL;
  }

  public static String printSeverityMarkdown(String severity) {
    StringBuilder sb = new StringBuilder();
    sb.append("*").append(StringUtils.upperCase(severity)).append("*").append(" - ");

    return sb.toString();
  }

  public static String printIssueNumberBySeverityMarkdown(List<Issue> report, String severity) {
    StringBuilder sb = new StringBuilder();
    long issueNumber = countIssuesBySeverity(report, severity);
    sb.append("| ").append(severity).append(" | ").append(issueNumber).append(" |")
        .append(NEW_LINE);

    return sb.toString();
  }

  public String printIssueListBySeverityMarkdown(List<Issue> report, String severity) {
    StringBuilder sb = new StringBuilder();

    Map<String, Issue> rules = getUniqueRulesBySeverity(report, severity);

    // applying squid:S2864 optimization
    for (Map.Entry<String, Issue> rule : rules.entrySet()) {
      Issue issue = rule.getValue();
      sb.append("| ").append(printIssueMarkdown(issue)).append(" |").append(NEW_LINE);
    }

    return sb.toString();
  }

  public List<String> printIssueMarkdown(List<Issue> issues) {
    return issues.stream().map(this::printIssueMarkdown).collect(Collectors.toList());
  }

  public String printIssueMarkdown(Issue issue) {
    StringBuilder sb = new StringBuilder();
    String message = issue.message();

    if (message == null) {
      return "No message";
    }

    String file = issuePathResolver.getIssuePath(issue);
    if (file != null) {
      String fileLink = link("`" + file + "`",
          stashURL + "/projects/" + pr.project() +
              "/repos/" + pr.repository() +
              "/pull-requests/" + pr.pullRequestId() +
              "/diff#" + file);
      message = message.replace(file, fileLink);

    }
    sb.append(MarkdownPrinter.printSeverityMarkdown(issue.severity()))
        .append(message)
        .append(" [")
        .append(link(issue.ruleKey().toString(),
            sonarQubeURL + "/" + CODING_RULES_RULE_KEY + issue.ruleKey()))
        .append("]");

    return sb.toString();
  }

  public String printReportMarkdown(List<Issue> allIssues, Project project) {
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

    List<Issue> coverageIssues = new ArrayList<>();
    List<Issue> generalIssues = new ArrayList<>();
    List<Issue> globalCoverageIssues = new ArrayList<>();

    for (Issue issue : allIssues) {
      if (isProjectWide(issue, project)) {
        globalCoverageIssues.add(issue);
      } else if (isCoverageEvolution(issue)) {
        coverageIssues.add(issue);
      } else {
        generalIssues.add(issue);
      }
    }

    if (allIssues.isEmpty()) {

      sb.append("### No new issues detected!");
      sb.append(NEW_LINE).append(NEW_LINE);

    } else {

      int issueNumber = allIssues.size();

      if (issueNumber >= issueThreshold) {
        sb.append("### Too many issues detected ");
        sb.append("(").append(issueNumber).append("/").append(issueThreshold).append(")");
        sb.append(": Issues cannot be displayed in Diff view.").append(NEW_LINE).append(NEW_LINE);
      }

      sb.append("| Total New Issues | ").append(issueNumber).append(" |").append(NEW_LINE);
      sb.append("|-----------------|------|").append(NEW_LINE);
      for (String severity : orderedSeverities) {
        sb.append(printIssueNumberBySeverityMarkdown(allIssues, severity));
      }
      sb.append(NEW_LINE).append(NEW_LINE);

      // Issue list
      sb.append("| Issues list |").append(NEW_LINE);
      sb.append("|------------|").append(NEW_LINE);
      /*
      List<IssueMetaInformation> uniqueSortedInformation = generalIssues.stream()
              .map(IssueMetaInformation::from)
              .distinct()
              .sorted(Comparator.comparing((item) -> Severity.ALL.indexOf(item.severity()), Comparator.reverseOrder()))
              .collect(Collectors.toList());
              */
      // FIXME printIssueMarkdown for this
      // make unique by  severity/
      for (String severity : orderedSeverities) {
        sb.append(printIssueListBySeverityMarkdown(generalIssues, severity));
      }
      sb.append(NEW_LINE).append(NEW_LINE);
    }

    sb.append(
        formatTableList("Project-wide coverage", printIssueMarkdown(globalCoverageIssues))
    );

    sb.append(
        formatTableList("Coverage", printIssueMarkdown(coverageIssues))
    );

    return sb.toString();
  }

  private String formatTableList(String caption, List<String> items) {
    if (items.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("| ").append(caption).append(" |").append(NEW_LINE);
    sb.append("|---------------|").append(NEW_LINE);
    for (String item : items) {
      sb.append("| ").append(item).append(" |").append(NEW_LINE);
    }

    return sb.toString();
  }

  private static String link(String title, String target) {
    return "[" + title + "](" + target + ")";
  }
}
