package org.sonar.plugins.stash;

import java.io.File;

public class DummyStashProjectBuilder extends StashProjectBuilder {
  private File projectBaseDir;

  public DummyStashProjectBuilder(File projectBaseDir) {
    this.projectBaseDir = projectBaseDir;
  }

  @Override
  public File getProjectBaseDir() {
    return projectBaseDir;
  }
}
