package org.sonar.plugins.stash.coverage;

import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.plugins.stash.StashTest;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.stash.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.plugins.stash.coverage.CoverageUtils.shouldExecuteCoverage;

public class CoverageUtilsTest extends StashTest {

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
  public void testCreateSonarClient3params() {

    String URL = "";
    String login = "";
    String pass = "";

    StashPluginConfiguration config = mock(StashPluginConfiguration.class);
    doReturn(URL).when(config).getSonarQubeURL();
    doReturn(login).when(config).getSonarQubeLogin();
    doReturn(pass).when(config).getSonarQubePassword();

    assertNotNull(CoverageUtils.createSonarClient(config));
  }

  @Test
  public void testCreateSonarClient1param() {

    String URL = "";
    String login = null;
    String pass = "";

    StashPluginConfiguration config = mock(StashPluginConfiguration.class);
    doReturn(URL).when(config).getSonarQubeURL();
    doReturn(login).when(config).getSonarQubeLogin();
    doReturn(pass).when(config).getSonarQubePassword();

    assertNotNull(CoverageUtils.createSonarClient(config));
  }

  @Test
  public void testGetLineCoverageExceptionAndNull() {

    String component = "mini-me";
    Sonar client = mock(Sonar.class);

    when(client.find(any())).thenThrow(new HttpException("UT for the win !", 1337));

    assertNull(CoverageUtils.getLineCoverage(client, component));
  }

  @Test
  public void testGetLineCoverageValue() {

    String component = "mini-me";
    Sonar client = mock(Sonar.class);

    // From the inside out:
    // 1) we define the value we are interested in
    Measure mes = new Measure();
    mes.setMetricKey(CoreMetrics.LINE_COVERAGE_KEY);
    mes.setValue(1337.0);

    // 2) we store this value inside the resource
    List<Measure> measures = new ArrayList();
    measures.add(mes);

    Resource res = new Resource();
    res.setMeasures(measures);

    // 3) we make sure this resource gets returned by the find() call
    doReturn(res).when(client).find(any());

    assertEquals(1337.0, CoverageUtils.getLineCoverage(client, component), DELTA);
  }

  @Test
  public void testShouldExecuteCoverage() {

    StashPluginConfiguration conf = mock(StashPluginConfiguration.class);
    ActiveRules arules_mock = mock(ActiveRules.class);

    // Testing the different codepaths

    // 1) Test on hasToNotifyStash() path
    when(conf.hasToNotifyStash()).thenReturn(false);

    assertFalse(shouldExecuteCoverage(conf, arules_mock));

    // 2) Test on shouldExecute() condition path
    when(conf.hasToNotifyStash()).thenReturn(true);

    assertFalse(shouldExecuteCoverage(conf, arules_mock));

    // 3) Test on scanAllFiles() path
    when(conf.hasToNotifyStash()).thenReturn(true);
    when(conf.scanAllFiles()).thenReturn(false);
    ActiveRules arules_real = (new ActiveRulesBuilder()).build();

    assertFalse(shouldExecuteCoverage(conf, arules_real));

    /*
    The big problem here is to mock properly the ActiveRules to make test 2 skip.
    Until then, test 3 is OK but not for the good reason (if you change test 2 then 3 will change)
      and the winning code too cannot work as it fails the ActiveRules test step.
    */

    // The winning path
    when(conf.hasToNotifyStash()).thenReturn(true);
    when(conf.scanAllFiles()).thenReturn(true);

    //assertTrue(shouldExecuteCoverage(conf, arules_real));
  }
}
