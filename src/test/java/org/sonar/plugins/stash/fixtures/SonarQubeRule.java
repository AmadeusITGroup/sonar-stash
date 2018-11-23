package org.sonar.plugins.stash.fixtures;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SonarQubeRule implements BeforeAllCallback, AfterAllCallback {
  private static final Logger LOGGER = LoggerFactory.getLogger(SonarQubeRule.class);

  private SonarQube sonarqube;

  public SonarQubeRule(SonarQube sonarqube) {
    this.sonarqube = sonarqube;
  }

  public void start() throws Exception {
    sonarqube.startAsync();
    sonarqube.waitForReady();
  }

  public SonarQube get() {
    return sonarqube;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    sonarqube.setUp();
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    sonarqube.stop();
  }
}
