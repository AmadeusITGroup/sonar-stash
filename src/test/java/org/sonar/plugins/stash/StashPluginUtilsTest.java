package org.sonar.plugins.stash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sonar.plugins.stash.StashPluginUtils.formatPercentage;

import org.junit.Test;

public class StashPluginUtilsTest {
  @Test
  public void testFormatPercentage() {
    assertEquals("10.9", formatPercentage(10.90));
    assertEquals("10.9", formatPercentage(10.94));
    assertEquals("11.0", formatPercentage(10.96));
    assertEquals("11.0", formatPercentage(11.0));
    assertEquals("31.3", formatPercentage(31.25));
  }

}
