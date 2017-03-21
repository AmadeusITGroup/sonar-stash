package org.sonar.plugins.stash.coverage;

public class CoverageUtils {
    private CoverageUtils() {}

    public static double calculateCoverage(int linesToCover, int uncoveredLines) {
        if (linesToCover == 0) {
            return 100;
        }

        return (1 - (((double) uncoveredLines) / linesToCover)) * 100;
    }
}
