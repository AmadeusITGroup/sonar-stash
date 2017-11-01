package org.sonar.plugins.stash.fixtures;

import com.google.common.base.Joiner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public class SonarScanner {
  protected Path installDir;

  public SonarScanner(Path installationDir) {
    this.installDir = installationDir;
  }

  protected Path getBinary(String name) {
    String ext = "";
    if (System.getProperty("os.name").equals("Windows")) {
      ext = ".bat";
    }
    return installDir.resolve("bin").resolve(name + ext);
  }

  private void addCliProperty(List<String> cli, String name, Object value) {
    cli.add("-D" + name + "=" + value);
  }

  public void scan(SonarQube sonarqube,
                   File baseDir,
                   List<File> sources,
                   String projectKey,
                   String projectName,
                   String projectVersion,
                   Properties properties) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(getBinary("sonar-scanner").toString())
        .directory(installDir.toFile())
        .inheritIO();
    List<String> command = pb.command();
    addCliProperty(command, "sonar.projectBaseDir", baseDir);
    addCliProperty(command, "sonar.host.url", sonarqube.getUrl());
    addCliProperty(command, "sonar.sources", Joiner.on(',').join(sources));
    addCliProperty(command, "sonar.projectKey", projectKey);
    addCliProperty(command, "sonar.projectName", projectName);
    addCliProperty(command, "sonar.projectVersion", projectVersion);
    addCliProperty(command, "sonar.analysis.mode", "issues");

    if (properties != null) {
      for (String p : properties.stringPropertyNames()) {
        addCliProperty(command, p, properties.getProperty(p));
      }
    }
    Process process = pb.start();
    process.waitFor();
    if (process.exitValue() != 0) {
      throw new IOException("Sonar Scanner failed with " + process.exitValue());
    }
  }
}
