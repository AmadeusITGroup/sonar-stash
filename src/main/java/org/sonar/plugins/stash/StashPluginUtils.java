package org.sonar.plugins.stash;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StashPluginUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashPluginUtils.class);

  private StashPluginUtils() {}

  public static String formatPercentage(double d) {
    DecimalFormat df = new DecimalFormat("0.0");
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(d);
  }
}
