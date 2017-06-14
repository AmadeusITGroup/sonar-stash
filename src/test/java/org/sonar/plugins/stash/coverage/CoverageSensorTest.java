package org.sonar.plugins.stash.coverage;

import org.junit.Test;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.StashPluginConfiguration;


import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;



public class CoverageSensorTest {
    
    private FileSystem FS                = mock(FileSystem.class);
    private ResourcePerspectives RP      = mock(ResourcePerspectives.class);
    private StashPluginConfiguration SPC = mock(StashPluginConfiguration.class);
    private ActiveRules AR               = mock(ActiveRules.class);
    private CoverageProjectStore CPS     = mock(CoverageProjectStore.class);
    
    CoverageSensor CoSe = new CoverageSensor(FS, RP, SPC, AR, CPS);
    
    
    public static String formatIssueMessage(String path, double coverage, double previousCoverage) {
        // make this available to all our tests
        return CoverageSensor.formatIssueMessage(path, coverage, previousCoverage);
    }

    @Test
    public void testRoundedFormatting() {

        // This test can fail with 50.2 instead of 50.3 if run with an older version of the JDK 8
        //    (failed with v25 & worked with v131)
        assertEquals(
                "Line coverage of file path/code/coverage lowered from 50.3% to 7.7%.",
                CoverageSensorTest.formatIssueMessage("path/code/coverage", 7.65, 50.29)
        );
    }

    @Test
    public void testToString() {
        assertEquals("Stash Plugin Coverage Sensor", CoSe.toString());
    }

    @Test
    public void testShouldExecuteOnProject() {
        Project proj = mock(Project.class);

        assertEquals(false, CoSe.shouldExecuteOnProject(proj));
    }
    
    @Test
    public void testAnalyse() {
    }
}
