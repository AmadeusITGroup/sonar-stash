package org.sonar.plugins.stash;

import org.sonar.api.batch.bootstrap.ProjectBuilder;

import java.io.File;

public class StashProjectBuilder extends ProjectBuilder {

  private final StashRequestFacade stashRequestFacade;

  public StashProjectBuilder(StashRequestFacade stashRequestFacade) {
    this.stashRequestFacade = stashRequestFacade;
  }
  
  @Override
  public void build(Context context) {
    File workingDir = new File(System.getProperty("user.dir"));
    stashRequestFacade.initialize(workingDir);
  }

}
