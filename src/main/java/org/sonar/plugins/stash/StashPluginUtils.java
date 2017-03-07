package org.sonar.plugins.stash;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;

public class StashPluginUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashPluginUtils.class);

  private StashPluginUtils() {}

  public static String formatPercentage(double d) {
    DecimalFormat df = new DecimalFormat("0.0");
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(d);
  }

  public static long countIssuesBySeverity(List<Issue> issues, final String severity) {
      return issues.stream().filter(i -> severity.equals(i.severity())).count();
  }

  public static Map<String, Issue> getUniqueRulesBySeverity(List<Issue> issues, final String severity) {
    Map<String, Issue> result = new HashMap<>();

    for (Issue issue : getIssuesBySeverity(issues, severity)) {
      result.put(issue.ruleKey().toString(), issue);
    }

    return result;
  }

  public static List<Issue> getIssuesBySeverity(List<Issue> issues, String severity) {
    return issues.stream().filter(i -> severity.equals(i.severity())).collect(Collectors.toList());
  }

}
