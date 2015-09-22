package org.sonar.plugins.stash;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Project;

public class InputFileCacheSensor implements Sensor {

  private final StashPluginConfiguration stashPluginConfiguration;
  private final FileSystem fileSystem;
  private final InputFileCache inputFileCache;

  public InputFileCacheSensor(StashPluginConfiguration stashPluginConfiguration, FileSystem fileSystem,
      InputFileCache inputFileCache) {
    this.stashPluginConfiguration = stashPluginConfiguration;
    this.fileSystem = fileSystem;
    this.inputFileCache = inputFileCache;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    for (InputFile inputFile : fileSystem.inputFiles(getAllPredicateFiles())) {
      inputFileCache.putInputFile(context.getResource(inputFile).getEffectiveKey(), inputFile);
    }
  }

  @Override
  public String toString() {
    return "Stash Plugin InputFile Cache";
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return stashPluginConfiguration.hasToNotifyStash();
  }

  private FilePredicate getAllPredicateFiles(){
    return fileSystem.predicates().all();
  }
  
}
