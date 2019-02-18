package org.sonar.plugins.stash;

import com.google.common.base.CharMatcher;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.issue.PostJobIssue;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.scanner.fs.InputProject;

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

  public static long countIssuesBySeverity(Collection<PostJobIssue> issues, final Severity severity) {
    return issues.stream().filter(i -> severity.equals(i.severity())).count();
  }

  public static boolean isProjectWide(PostJobIssue issue) {
    InputComponent ic = issue.inputComponent();
    if (!(ic instanceof InputProject)) {
      return false;
    }
    InputProject im = (InputProject) ic;
    if (im.key() == null) {
      return false;
    }
    return CharMatcher.is(':').countIn(im.key()) == 0;
  }

  public static PluginInfo getPluginInfo() {
    Properties props = new Properties();
    try {
      props.load(StashPluginUtils.class.getClassLoader().getResourceAsStream("org/sonar/plugins/stash/sonar-stash.properties"));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return new PluginInfo(
        props.getProperty("project.name"),
        props.getProperty("project.version")
    );
  }

  public static String removeEnd(String s, String suffix) {
    if (s.endsWith(suffix)) {
      return removeEnd(s.substring(0, s.length() - suffix.length()), suffix);
    }
    return s;
  }
}
