package org.sonar.plugins.stash;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonar.plugins.stash.StashPluginUtils.formatPercentage;
import static org.sonar.plugins.stash.StashPluginUtils.roundedPercentageGreaterThan;


public class StashPluginUtilsTest {

  @Test
  public void testFormatPercentage() {
    assertEquals("10.9", formatPercentage(10.90));
    assertEquals("10.9", formatPercentage(10.94));
    assertEquals("11.0", formatPercentage(10.96));
    assertEquals("11.0", formatPercentage(11.0));
    assertEquals("31.3", formatPercentage(31.25));
    assertEquals("50.3", formatPercentage(50.29));
    // This test can fail with 50.2 instead of 50.3 if run with an older version of the JDK 8
    //    (failed with v25 & worked with v131)
  }

  @Test
  public void testRoundedPercentageGreaterThan() {
    assertTrue(roundedPercentageGreaterThan(0.1, 0.0));
    assertTrue(roundedPercentageGreaterThan(0.2, 0.1));
    assertTrue(roundedPercentageGreaterThan(1, 0.1));
    assertTrue(roundedPercentageGreaterThan(1.1, 0.1));

    assertFalse(roundedPercentageGreaterThan(0.0, 0.1));
    assertFalse(roundedPercentageGreaterThan(0.1, 0.2));
    assertFalse(roundedPercentageGreaterThan(1.1, 1.2));

    assertFalse(roundedPercentageGreaterThan(1.01, 1.00));
    assertFalse(roundedPercentageGreaterThan(1.04, 1.00));

    assertTrue(roundedPercentageGreaterThan(1.05, 1.00));
  }
}
