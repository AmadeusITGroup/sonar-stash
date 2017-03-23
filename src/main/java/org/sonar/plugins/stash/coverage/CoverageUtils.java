package org.sonar.plugins.stash.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public final class CoverageUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoverageUtils.class);

    private CoverageUtils() {}

    public static double calculateCoverage(int linesToCover, int uncoveredLines) {
        if (linesToCover == 0) {
            return 100;
        }

        return (1 - (((double) uncoveredLines) / linesToCover)) * 100;
    }

    public static Sonar createSonarClient(StashPluginConfiguration config) {
        String url = config.getSonarQubeURL();
        String login = config.getSonarQubeLogin();
        String password = config.getSonarQubePassword();
        if (login != null) {
            return Sonar.create(url, login, password);
        } else {
            return Sonar.create(url);
        }
    }

    public static Double getLineCoverage(Sonar client, String component) {
        Resource resource = null;
        try {
            resource = client.find(ResourceQuery.createForMetrics(component, CoreMetrics.LINE_COVERAGE_KEY));
        } catch (HttpException e) {
            LOGGER.error("Could not fetch previous coverage for component {}", component, e);
        }
        if (resource == null) {
            return null;
        } else {
            return resource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
        }
    }
}
