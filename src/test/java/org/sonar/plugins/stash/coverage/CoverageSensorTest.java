package org.sonar.plugins.stash.coverage;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CoverageSensorTest {
    public static String formatIssueMessage(String path, double coverage, double previousCoverage) {
        // make this available to all our tests
        return CoverageSensor.formatIssueMessage(path, coverage, previousCoverage);
    }

    @Test
    public void testRoundedFormatting() {
        assertEquals(
                "Line coverage of file path/code/coverage lowered from 50.3% to 7.7%.",
                CoverageSensorTest.formatIssueMessage("path/code/coverage", 7.65, 50.29)
        );

    }
}
