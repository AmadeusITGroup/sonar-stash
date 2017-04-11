package org.sonar.plugins.stash.coverage;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sonar.plugins.stash.coverage.CoverageRule.decreasingLineCoverageRule;
import static org.sonar.plugins.stash.coverage.CoverageRule.isDecreasingLineCoverage;

public class CoverageRuleTest {

    @Test
    public void testDecreasingLineCoverageRule() {
        assertEquals("coverageEvolution-neutral:decreasingLineCoverage", decreasingLineCoverageRule("neutral").toString());
    }

    @Test
    public void testIsDecreasingLineCoverage() {
        assertFalse(isDecreasingLineCoverage("foo:bar"));
        assertFalse(isDecreasingLineCoverage("xxx-neutral:decreasingLineCoverage"));
        assertFalse(isDecreasingLineCoverage("coverageEvolution-neutral:xxx"));

        assertTrue(isDecreasingLineCoverage("coverageEvolution-neutral:decreasingLineCoverage"));
        assertTrue(isDecreasingLineCoverage("coverageEvolution-java:decreasingLineCoverage"));
    }


}
