package org.sonar.plugins.stash.fixtures;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class SonarQubeRule implements TestRule {
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
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          sonarqube.setUp();
          base.evaluate();
        } finally {
          sonarqube.stop();
        }
      }

    };
  }
}
