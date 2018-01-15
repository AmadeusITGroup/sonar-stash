package org.sonar.plugins.stash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;

import java.io.File;

public class StashProjectBuilder extends ProjectBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(StashProjectBuilder.class);
    
  private File projectBaseDir;

  @Override
  public void build(Context context) {
    projectBaseDir = context.projectReactor().getRoot().getBaseDir();
    LOGGER.debug("Project base dir is {}", projectBaseDir);
  }

  public File getProjectBaseDir() {
    return projectBaseDir;
  }

}
