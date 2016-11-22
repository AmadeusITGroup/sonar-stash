package org.sonar.plugins.stash;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StashPluginUtilsTest {

  @Test
  public void testFormatDouble() {
    assertTrue(StashPluginUtils.formatDouble(10.90) == 10.9);
    assertTrue(StashPluginUtils.formatDouble(10.94) == 10.9);
    assertTrue(StashPluginUtils.formatDouble(10.96) == 11.0);
    assertTrue(StashPluginUtils.formatDouble(11.0) == 11.0);
    assertTrue(StashPluginUtils.formatDouble(11) == 11.0);
  }
  
}
