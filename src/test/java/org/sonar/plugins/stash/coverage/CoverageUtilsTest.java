package org.sonar.plugins.stash;

import java.util.ArrayList;

import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;

import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.plugins.stash.coverage.CoverageRule;

import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonar.plugins.stash.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.plugins.stash.coverage.CoverageUtils.createSonarClient;
import static org.sonar.plugins.stash.coverage.CoverageUtils.getLineCoverage;
import static org.sonar.plugins.stash.coverage.CoverageUtils.shouldExecuteCoverage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;


import org.junit.Test;

public class CoverageUtilsTest {

  private static final double DELTA = 1e-6;

  @Test
  public void testCalculateCoverage() {
    assertEquals(100.0, calculateCoverage(0, 0), DELTA); // special case
    assertEquals(50.0, calculateCoverage(100, 50), DELTA);
    assertEquals(94.5, calculateCoverage(1000, 55), DELTA);
    assertEquals(87.3, calculateCoverage(1000, 127), DELTA);
    assertEquals(31.09, calculateCoverage(10000, 6891), DELTA);
    assertEquals(0.27, calculateCoverage(10000, 9973), DELTA);

    // not a wanted case but it works with current implementation (so good future exception test)
    assertEquals(200, calculateCoverage(100, -100), DELTA);
  }

  @Test
  public void testCreateSonarClient() {

    String URL="";
    String login="";
    String pass="";

    StashPluginConfiguration config = mock(StashPluginConfiguration.class);
    doReturn(URL).when(config).getSonarQubeURL();
    doReturn(login).when(config).getSonarQubeLogin();
    doReturn(pass).when(config).getSonarQubePassword();
    
    /*
    The Sonar object is a bit trickier to mock than an internal class.
        To be implemented.
    */
}

  @Test
  public void testGetLineCoverage() {

    // Double getLineCoverage(Sonar client, String component)
    String component = "mini-me";
    Sonar client = mock(Sonar.class);

    /*
    The Sonar object is a bit trickier to mock than an internal class.
        To be implemented.
    */
  }
  
  @Test
  public void testShouldExecuteCoverage() {

    StashPluginConfiguration conf = mock(StashPluginConfiguration.class);
    ActiveRules arules = mock(ActiveRules.class);

    // Testing the different codepaths

    // 1) Test on hasToNotifyStash() path
    when(conf.hasToNotifyStash()).thenReturn(false);

    assertFalse(shouldExecuteCoverage(conf, arules));

    // 2) Test on shouldExecute() condition path
    when(conf.hasToNotifyStash()).thenReturn(true);

    assertFalse(shouldExecuteCoverage(conf, arules));

    // 3) Test on scanAllFiles() path
    when(conf.hasToNotifyStash()).thenReturn(true);
    when(conf.scanAllFiles()).thenReturn(false);

    assertFalse(shouldExecuteCoverage(conf, arules));

    /*
    The big problem here is to mock properly the ActiveRules to make test 2 skip.
    Until then, test 3 is OK but not for the good reason (if you change test 2 then 3 will change)
      and the winning code too cannot work as it fails the ActiveRules test step.
    */

    // The winning path
    when(conf.hasToNotifyStash()).thenReturn(true);
    when(conf.scanAllFiles()).thenReturn(true);
    
    //assertTrue(shouldExecuteCoverage(conf, arules));
  }
}
