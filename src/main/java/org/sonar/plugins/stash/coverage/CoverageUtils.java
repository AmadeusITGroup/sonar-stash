package org.sonar.plugins.stash.coverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public final class CoverageUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageUtils.class);
  public static final int MAX_PERCENTAGE  = 100;

  private CoverageUtils() {
    // Hiding implicit public constructor with an explicit private one (squid:S1118)
  }

  public static double calculateCoverage(int linesToCover, int uncoveredLines) {
    if (linesToCover == 0) {
      return MAX_PERCENTAGE;
    }

    return (1 - (((double)uncoveredLines) / linesToCover)) * MAX_PERCENTAGE;
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
    // It would be worth checking if the call to get the resource is able to return null.
    //    Otherwise, we can transfer this "return null" statement in the catch and save an if.
    if (resource == null) {
      return null;
    } else {
      return resource.getMeasureValue(CoreMetrics.LINE_COVERAGE_KEY);
    }
  }

  public static boolean shouldExecuteCoverage(StashPluginConfiguration config, ActiveRules activeRules) {
    // We only execute when run in stash reporting mode
    // This indicates we are running in preview mode,
    // I don't know how we should behave during a normal scan
    if (!config.hasToNotifyStash()) {
      return false;
    }

    if (!CoverageRule.shouldExecute(activeRules)) {
      return false;
    }

    if (!config.scanAllFiles()) {
      LOGGER.warn("Not scanning all files, coverage features will be unreliable and will be disabled");
      return false;
    }

    return true;
  }
}
