package org.sonar.plugins.stash.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
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

// We have to execute after all coverage sensors, otherwise we are not able to read their measurements
@Phase(name = Phase.Name.POST)
public class CoverageSensor implements org.sonar.api.batch.Sensor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageSensor.class);

    private final FileSystem fileSystem;
    private final ResourcePerspectives perspectives;
    private final StashPluginConfiguration config;
    private Double projectCoverage = null;
    private Double previousProjectCoverage = null;
    private final String severity;


    public CoverageSensor(FileSystem fileSystem, ResourcePerspectives perspectives, StashPluginConfiguration config) {
        this.fileSystem = fileSystem;
        this.perspectives = perspectives;
        this.config = config;
        this.severity = config.getCodeCoverageSeverity();
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
                    org.sonar.wsclient.services.Resource wsResource = sonar.find(ResourceQuery.createForMetrics(fileResource.getKey(), CoreMetrics.LINE_COVERAGE_KEY));
                    if (wsResource != null) {
                        previousCoverage = wsResource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
                    }
                } catch (HttpException e) {
                    LOGGER.error("Could not fetch previous coverage for file {}", f, e);
                }

                if (previousCoverage == -1) {
                    continue;
                }

                double coverage = calculateCoverage(linesToCover, uncoveredLines);

                totalLinesToCover += linesToCover;
                totalUncoveredLines += uncoveredLines;

                // handle rounded coverage from API
                if (previousCoverage > (coverage + 0.1)) {
                    Issuable issuable = perspectives.as(Issuable.class, f);
                    if (issuable != null) {
                        String message = MessageFormat.format("Code coverage of file {0} lowered from {1,number,#.##}% to {2,number,#.##}%",
                                f.relativePath(), previousCoverage, coverage);
                        Issue issue = issuable.newIssueBuilder()
                                .severity(severity)
                                .ruleKey(CoverageRule.decreasingLineCoverageRule(f.language()))
                                .message(message)
                                .build();
                        LOGGER.warn("adding: " + issue.message());
                        issuable.addIssue(issue);
                    }
                }
            }
        }

        this.projectCoverage = calculateCoverage(totalLinesToCover, totalUncoveredLines);

        org.sonar.wsclient.services.Resource wsResource = sonar.find(ResourceQuery.createForMetrics(module.getEffectiveKey(), CoreMetrics.LINE_COVERAGE_KEY));
        if (wsResource != null) {
            previousProjectCoverage = wsResource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
        }
    }

    @Override
    public String toString() {
        return "Stash Plugin Coverage Sensor";
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return (config.hasToNotifyStash() && null != severity && !severity.isEmpty());
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