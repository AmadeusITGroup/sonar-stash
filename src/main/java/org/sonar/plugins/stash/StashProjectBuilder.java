package org.sonar.plugins.stash;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;

public class StashProjectBuilder extends ProjectBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashProjectBuilder.class);

  private File workingDir;

  @Override
  public void build(Context context) {
    workingDir = new File(System.getProperty("user.dir"));
    LOGGER.debug("Current working directory {}", workingDir);
  }

  public File getWorkingDir() {
    return workingDir;
  }

}
