package org.sonar.plugins.stash.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ResourceQuery;

import static org.sonar.plugins.stash.coverage.CoverageUtils.calculateCoverage;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class CoverageProjectStore implements BatchComponent, Sensor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageProjectStore.class);

    private Double previousProjectCoverage = null;
    private int linesToCover = 0;
    private int uncoveredLines = 0;

    private final StashPluginConfiguration config;
    private ActiveRules activeRules;

    public CoverageProjectStore(StashPluginConfiguration config, ActiveRules activeRules) {
        this.config = config;
        this.activeRules = activeRules;
    }

    public Double getProjectCoverage() {
        return calculateCoverage(linesToCover, uncoveredLines);
    }

    public Double getPreviousProjectCoverage() {
        return this.previousProjectCoverage;
    }

    @Override
    public void analyse(Project module, SensorContext context) {
        String sonarQubeURL = config.getSonarQubeURL();
        Sonar sonar = Sonar.create(sonarQubeURL, config.getSonarQubeLogin(), config.getSonarQubePassword());

        org.sonar.wsclient.services.Resource wsResource = sonar.find(ResourceQuery.createForMetrics(module.getEffectiveKey(), CoreMetrics.LINE_COVERAGE_KEY));
        if (wsResource != null) {
            previousProjectCoverage = wsResource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
        }
    }

    public boolean shouldExecuteOnProject(Project project) {
        return config.hasToNotifyStash() && CoverageRule.shouldExecute(activeRules);
    }

    public void updateMeasurements(int linesToCover, int uncoveredLines) {
        this.linesToCover += linesToCover;
        this.uncoveredLines += uncoveredLines;
    }
}
