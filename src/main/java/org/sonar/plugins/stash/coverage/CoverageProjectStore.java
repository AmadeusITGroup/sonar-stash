package org.sonar.plugins.stash.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.wsclient.Sonar;

import static org.sonar.plugins.stash.coverage.CoverageUtils.calculateCoverage;
import static org.sonar.plugins.stash.coverage.CoverageUtils.createSonarClient;
import static org.sonar.plugins.stash.coverage.CoverageUtils.getLineCoverage;

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
        Sonar sonar = createSonarClient(config);
        if (config.scanAllFiles()) {
            previousProjectCoverage = getLineCoverage(sonar, module.getEffectiveKey());
        } else {
            LOGGER.info("Not scanning all files, project-wide coverage disabled");
        }
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return config.hasToNotifyStash() && CoverageRule.shouldExecute(activeRules);
    }

    public void updateMeasurements(int linesToCover, int uncoveredLines) {
        this.linesToCover += linesToCover;
        this.uncoveredLines += uncoveredLines;
    }
}
