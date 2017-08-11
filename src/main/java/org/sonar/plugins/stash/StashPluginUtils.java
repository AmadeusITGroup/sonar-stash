package org.sonar.plugins.stash;

import org.sonar.api.issue.Issue;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Objects;
import org.sonar.api.resources.Project;

public final class StashPluginUtils {

  private StashPluginUtils() {}

  public static String formatPercentage(double d) {

    // Defining that our percentage is precise down to 0.1%
    DecimalFormat df = new DecimalFormat("0.0");

    // Protecting this method against non-US locales that would not use '.' as decimal separation
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    decimalFormatSymbols.setDecimalSeparator('.');
    df.setDecimalFormatSymbols(decimalFormatSymbols);

    // Making sure that we round the 0.1% properly out of the double value
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(d);
  }

  public static boolean roundedPercentageGreaterThan(double left, double right) {
    return (left > right) && !formatPercentage(left).equals(formatPercentage(right));
  }

  public static long countIssuesBySeverity(List<Issue> issues, final String severity) {
    return issues.stream().filter(i -> severity.equals(i.severity())).count();
  }

  public static boolean isProjectWide(Issue issue, Project project) {
    String k = issue.componentKey();
    return (k != null && Objects.equals(k, project.getKey()));
  }
}
