package org.sonar.plugins.stash.coverage;

import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.StashPluginConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;


public class CoverageProjectStoreTest {

  private static final double DELTA = 1e-6;

  private StashPluginConfiguration SPC = mock(StashPluginConfiguration.class);
  private ActiveRules AR = mock(ActiveRules.class);

  private CoverageProjectStore CoProSto = new CoverageProjectStore(SPC, AR);


  @Test
  public void testGetProjectCoverage() {

    assertEquals(100.0, CoProSto.getProjectCoverage(), DELTA);
  }

  @Test
  public void testGetPreviousProjectCoverage() {

    assertNull(CoProSto.getPreviousProjectCoverage());
  }

  @Test
  public void testAnalyse() {
    // Impossible to do a UT as the client cannot be mocked...
  }

  @Test
  public void testShouldExecuteOnProject() {

    Project proj = mock(Project.class);
    assertFalse(CoProSto.shouldExecuteOnProject(proj));
  }

  @Test
  public void testUpdateMeasurements() {

    CoProSto.updateMeasurements(37, 13);  // Little-endian leet ;o))

    assertEquals(64.86486486486487, CoProSto.getProjectCoverage(), DELTA);
  }
}
