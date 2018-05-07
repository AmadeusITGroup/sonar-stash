package org.sonar.plugins.stash.issue;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
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

      List<String> uniqueSortedInformation = allIssues.stream()
              .filter(distinctByKey(PostJobIssue::ruleKey))
              .sorted(Comparator.comparing(PostJobIssue::severity).reversed())
              .map(this::printIssueMarkdown)
              .collect(Collectors.toList());
      sb.append(
          formatTableList(uniqueSortedInformation, this.getMappedIssuesToFiles(allIssues))
      );
    }

    return sb.toString();
  }

  private String formatTableList(List<String> items, ListMultimap<String, String> issueToFilesMap) {
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
    for (String item : items) {
        sb.append("| ").append(item).append(" |").append(NEW_LINE);
        if (this.includeFilesInOverview > 0 && issueToFilesMap.containsKey(item)) {
            String files = String.join(", ", issueToFilesMap.get(item));
            sb.append("| &nbsp;&nbsp; *Files: ").append(files).append("* |").append(NEW_LINE);
        }
    }

    return sb.toString();
  }

  private ListMultimap<String, String> getMappedIssuesToFiles(Collection<PostJobIssue> allIssues) {
    ListMultimap<String, String> issueToFilesMap = ArrayListMultimap.create();
    if (this.includeFilesInOverview <= 0) {
      return issueToFilesMap;
    }

    for (PostJobIssue issue : allIssues) {
      String title = this.printIssueMarkdown(issue);

      Integer issueLine = issue.line();
      if (issueLine == null) {
        issueLine = 0;
      }

      issueToFilesMap.get(title).add(this.issuePathResolver.getIssuePath(issue) + ":" + issueLine);
    }

    this.sortMappedIssuesToFiles(issueToFilesMap);

    return issueToFilesMap;
  }

  private void sortMappedIssuesToFiles(ListMultimap<String, String> issueToFilesMap) {
    for (String key : Lists.newArrayList(issueToFilesMap.keySet())) {
      List<String> files = issueToFilesMap.get(key);
      files.sort(Comparator.comparing(String::length).thenComparing(String::compareTo));

      if (files.size() >= this.includeFilesInOverview) {
        files.subList(this.includeFilesInOverview, files.size()).clear();
        files.add("...");
      }
    }
  }

  private String repeatString(int n, String s) {
    return String.join("", Collections.nCopies(n, s));
  }

  private static String link(String title, String target) {
    return "[" + title + "](" + target + ")";
  }

  private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
    Set<Object> seen = Collections.newSetFromMap(new ConcurrentHashMap<>());
    return t -> seen.add(keyExtractor.apply(t));
  }
}
