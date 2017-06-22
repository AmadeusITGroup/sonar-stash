package org.sonar.plugins.stash.fixtures;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class MavenSonarFixtures {
  protected static SonarQube sonarqube = null;
  protected static SonarScanner sonarScanner = null;

  public synchronized static SonarQube getSonarqube(int port) {
    if (sonarqube == null) {
      String outputDir = System.getProperty("test.sonarqube.dist.outputdir");
      if (outputDir == null) {
        throw new IllegalArgumentException("Location of unpacked SonarQube is not specified");
      }
      String version = System.getProperty("test.sonarqube.dist.version");
      Path installationDir = FileSystems.getDefault().getPath(outputDir, "sonarqube-" + version);
      sonarqube = new SonarQube(installationDir, port);
    }
    return sonarqube;
  }

  public synchronized static SonarScanner getSonarScanner() {
    if (sonarScanner == null) {
      String outputDir = System.getProperty("test.sonarscanner.dist.outputdir");
      if (outputDir == null) {
        throw new IllegalArgumentException("Location of unpacked Sonar Scanner is not specified");
      }
      String version = System.getProperty("test.sonarscanner.dist.version");
      Path installationDir = FileSystems.getDefault().getPath(outputDir, "sonar-scanner-" + version);
      sonarScanner = new SonarScanner(installationDir);
    }
    return sonarScanner;
  }
}
