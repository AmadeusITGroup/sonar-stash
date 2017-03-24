package org.sonar.plugins.stash;

import org.sonar.api.batch.bootstrap.ProjectBuilder;

import java.io.File;

public class StashProjectBuilder extends ProjectBuilder {
  private File workingDir;

  @Override
  public void build(Context context) {
    workingDir = new File(System.getProperty("user.dir"));
  }

  public File getWorkingDir() {
    return workingDir;
  }

}
