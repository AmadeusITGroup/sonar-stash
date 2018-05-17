package org.sonar.plugins.stash.issue;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.IssuePathResolver;

import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;

public final class MarkdownPrinter {

  private static final String NEW_LINE = "\n";
  private static final String CODING_RULES_RULE_KEY = "coding_rules#rule_key=";

  private int issueThreshold;
  private String sonarQubeURL;
  private int includeFilesInOverview;
  private IssuePathResolver issuePathResolver;

  private Severity[] orderedSeverities;

  public MarkdownPrinter(
    int issueThreshold, String sonarQubeURL, int includeFilesInOverview, IssuePathResolver issuePathResolver
  ) {
    this.issueThreshold = issueThreshold;
    this.sonarQubeURL = sonarQubeURL;
    this.includeFilesInOverview = includeFilesInOverview;
    this.issuePathResolver = issuePathResolver;

    orderedSeverities = Severity.values();
    Arrays.sort(orderedSeverities, Comparator.reverseOrder());
  }

  public static String printSeverityMarkdown(org.sonar.api.batch.rule.Severity severity) {
    StringBuilder sb = new StringBuilder();
    sb.append("*").append(severity.name().toUpperCase()).append("*").append(" - ");

    return sb.toString();
  }

  public static String printIssueNumberBySeverityMarkdown(Collection<PostJobIssue> report, Severity severity) {
    StringBuilder sb = new StringBuilder();
    long issueNumber = countIssuesBySeverity(report, severity);
    sb.append("| ").append(severity).append(" | ").append(issueNumber).append(" |")
        .append(NEW_LINE);

    return sb.toString();
  }

  public String printIssueMarkdown(PostJobIssue issue) {
    StringBuilder sb = new StringBuilder();
    String message = issue.message();

    if (message == null) {
      return "No message";
    }

    sb.append(MarkdownPrinter.printSeverityMarkdown(issue.severity()))
        .append(message)
        .append(" [")
        .append(link(issue.ruleKey().toString(),
            sonarQubeURL + "/" + CODING_RULES_RULE_KEY + issue.ruleKey()))
        .append("]");

    return sb.toString();
  }

  public String printReportMarkdown(Collection<PostJobIssue> allIssues) {
    StringBuilder sb = new StringBuilder("## SonarQube analysis Overview");
    sb.append(NEW_LINE);

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
      for (Severity severity : orderedSeverities) {
        sb.append(printIssueNumberBySeverityMarkdown(allIssues, severity));
      }

      ListMultimap<RuleKey, PostJobIssue> uniqueInformation = allIssues.stream().collect(Multimaps.toMultimap(PostJobIssue::ruleKey, Function.identity(), ArrayListMultimap::create));

      List<Map.Entry<RuleKey, List<PostJobIssue>>> uniqueSortedInformation = Multimaps.asMap(uniqueInformation)
              .entrySet().stream()
              .sorted(bySeverity.reversed())
              .collect(Collectors.toList());
      sb.append(
          formatTableList(uniqueSortedInformation));
    }

    return sb.toString();
  }

  private String formatTableList(List<Map.Entry<RuleKey, List<PostJobIssue>>> items) {
    if (items.isEmpty()) {
      return "";
    }

    String caption = "Issues list";
    StringBuilder sb = new StringBuilder();
    sb.append(NEW_LINE).append(NEW_LINE);
    sb.append("| ").append(caption).append(" |").append(NEW_LINE);
    sb.append("|");
    sb.append(repeatString(caption.length() + 2, "-"));
    sb.append("|");
    sb.append(NEW_LINE);
    for (Map.Entry<RuleKey, List<PostJobIssue>> item : items) {
      List<PostJobIssue> issues = item.getValue();
      sb.append("| ").append(printIssueMarkdown(issues.get(0))).append(" |").append(NEW_LINE);
        if (this.includeFilesInOverview > 0) {
            sb.append("| &nbsp;&nbsp; *Files: ").append(fileNameList(issues)).append("* |").append(NEW_LINE);
        }
    }

    return sb.toString();
  }

  private String fileNameList(List<PostJobIssue> issues) {
    List<String> names = new ArrayList<>();

    issues.sort(issueFormatComparator);

    for (PostJobIssue issue: issues.subList(0, Math.min(includeFilesInOverview, issues.size()))) {
      names.add(String.format("%s:%s", issuePathResolver.getIssuePath(issue), issue.line()));
    }
    if (issues.size() > includeFilesInOverview) {
      names.add("...");
    }
    return String.join(", ", names);
  }

  private String repeatString(int n, String s) {
    return String.join("", Collections.nCopies(n, s));
  }

  private static String link(String title, String target) {
    return "[" + title + "](" + target + ")";
  }

  private static Comparator<? super Entry<RuleKey, ? extends Collection<PostJobIssue>>> bySeverity = Comparator.comparing(
      // List invariant is guaranteed by docs
      e -> ((List<PostJobIssue>) e.getValue()).get(0).severity());

  private Comparator<PostJobIssue> fileNameLength = Comparator
      .comparing(i -> issuePathResolver.getIssuePath(i).length());

  private Comparator<PostJobIssue> issueLine = Comparator
      .comparing(PostJobIssue::line);

  private Comparator<PostJobIssue> fileNameLexical = Comparator
      .comparing(i -> issuePathResolver.getIssuePath(i));

  private Comparator<PostJobIssue> issueFormatComparator =
      fileNameLength
      .thenComparing(issueLine)
      .thenComparing(fileNameLexical);
}
