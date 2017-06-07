package org.sonar.plugins.stash;

public class SonarSettings {

    private String sonarQubeURL;
    private int issueThreshold;
    private Double projectCoverage;
    private Double previousProjectCoverage;

    public SonarSettings(String sonarQubeURL, int issueThreshold,
                          Double projectCoverage, Double previousProjectCoverage) {

        this.sonarQubeURL   = sonarQubeURL;
        this.issueThreshold = issueThreshold;
        this.projectCoverage = projectCoverage;
        this.previousProjectCoverage = previousProjectCoverage;
    }

    public String sonarQubeURL() {
        return sonarQubeURL;
    }

    public int issueThreshold() {
        return issueThreshold;
    }

    public Double projectCoverage() {
        return projectCoverage;
    }

    public Double previousProjectCoverage() {
        return previousProjectCoverage;
    }
}
