package org.sonar.plugins.stash;

import org.sonar.api.batch.bootstrap.ProjectBuilder;

import java.io.File;

public class StashProjectBuilder extends ProjectBuilder {
  private File projectBaseDir;

  @Override
  public void build(Context context) {
    projectBaseDir = context.projectReactor().getRoot().getBaseDir();
  }

  public File getProjectBaseDir() {
    return projectBaseDir;
  }

}
