package org.sonar.plugins.stash.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.ResourceQuery;

import java.text.MessageFormat;

import static org.sonar.plugins.stash.StashPluginUtils.formatPercentage;
import static org.sonar.plugins.stash.StashPluginUtils.roundedPercentageGreaterThan;

// We have to execute after all coverage sensors, otherwise we are not able to read their measurements
@Phase(name = Phase.Name.POST)
public class CoverageSensor implements Sensor, BatchComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageSensor.class);

    private final FileSystem fileSystem;
    private final ResourcePerspectives perspectives;
    private final StashPluginConfiguration config;
    private Double projectCoverage = null;
    private Double previousProjectCoverage = null;
    private ActiveRules activeRules;


    public CoverageSensor(FileSystem fileSystem, ResourcePerspectives perspectives, StashPluginConfiguration config, ActiveRules activeRules) {
        this.fileSystem = fileSystem;
        this.perspectives = perspectives;
        this.config = config;
        this.activeRules = activeRules;
    }

    @Override
    public void analyse(Project module, SensorContext context) {
        String sonarQubeURL = config.getSonarQubeURL();
        Sonar sonar = Sonar.create(sonarQubeURL);

        int totalLinesToCover = 0;
        int totalUncoveredLines = 0;

        for (InputFile f : fileSystem.inputFiles(fileSystem.predicates().all())) {
            LOGGER.warn(f.absolutePath());
            Integer linesToCover = null;
            Integer uncoveredLines = null;

            Resource fileResource = context.getResource(f);
            Measure<Integer> linesToCoverMeasure = context.getMeasure(fileResource, CoreMetrics.LINES_TO_COVER);
            if (linesToCoverMeasure != null) {
                linesToCover = linesToCoverMeasure.value();
            }

            Measure<Integer> uncoveredLinesMeasure = context.getMeasure(fileResource, CoreMetrics.UNCOVERED_LINES);
            if (uncoveredLinesMeasure != null) {
                uncoveredLines = uncoveredLinesMeasure.value();
            }

            // get lines_to_cover, uncovered_lines
            if ((linesToCover != null) && (uncoveredLines != null)) {
                double previousCoverage = -1;

                try {
                    org.sonar.wsclient.services.Resource wsResource = sonar.find(ResourceQuery.createForMetrics(fileResource.getEffectiveKey(), CoreMetrics.LINE_COVERAGE_KEY));
                    if (wsResource != null) {
                        previousCoverage = wsResource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
                    }
                } catch (HttpException e) {
                    LOGGER.error("Could not fetch previous coverage for file {}", f, e);
                }


                double coverage = calculateCoverage(linesToCover, uncoveredLines);

                totalLinesToCover += linesToCover;
                totalUncoveredLines += uncoveredLines;

                if (previousCoverage == -1) {
                    continue;
                }

                // The API returns the coverage rounded.
                // So we can only report anything if the rounded value has changed,
                // otherwise we could report false positives.
                if (roundedPercentageGreaterThan(previousCoverage, coverage)) {
                    addIssue(f, coverage, previousCoverage);
                }
            }
        }

        this.projectCoverage = calculateCoverage(totalLinesToCover, totalUncoveredLines);

        org.sonar.wsclient.services.Resource wsResource = sonar.find(ResourceQuery.createForMetrics(module.getEffectiveKey(), CoreMetrics.LINE_COVERAGE_KEY));
        if (wsResource != null) {
            previousProjectCoverage = wsResource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
        }
    }

    private void addIssue(InputFile file, double coverage, double previousCoverage) {
        Issuable issuable = perspectives.as(Issuable.class, file);
        if (issuable == null) {
            LOGGER.warn("Could not get a perspective of Issuable to create an issue for {}, skipping", file);
            return;
        }

        String message = formatIssueMessage(file.relativePath(), coverage, previousCoverage);
        Issue issue = issuable.newIssueBuilder()
                .ruleKey(CoverageRule.decreasingLineCoverageRule(file.language()))
                .message(message)
                .build();
        issuable.addIssue(issue);
    }

    static String formatIssueMessage(String path, double coverage, double previousCoverage) {
        return MessageFormat.format("Line coverage of file {0} lowered from {1}% to {2}%.",
                                    path, formatPercentage(previousCoverage), formatPercentage(coverage));
    }

    @Override
    public String toString() {
        return "Stash Plugin Coverage Sensor";
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        // We only execute when run in stash reporting mode
        // This indicates we are running in preview mode,
        // I don't know how we should behave during a normal scan
        return config.hasToNotifyStash() && CoverageRule.shouldExecute(activeRules);
    }

    private static double calculateCoverage(int linesToCover, int uncoveredLines) {
        if (linesToCover == 0) {
            return 100;
        }

        return (1 - (((double) uncoveredLines) / linesToCover)) * 100;
    }

    public Double getProjectCoverage() {
        return this.projectCoverage;
    }
    public Double getPreviousProjectCoverage() {
        return this.previousProjectCoverage;
    }
}
