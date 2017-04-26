package org.sonar.plugins.stash;

import com.google.common.base.Optional;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

/*
 * FIXME this should not be necessary, the new plugin API gives us direct access to the InputFile of an issue
 */
public class InputFileCacheSensor implements Sensor, BatchComponent {
    private final StashPluginConfiguration stashPluginConfiguration;
    private final InputFileCache inputFileCache;
    private final FileSystem fileSystem;

    public InputFileCacheSensor(StashPluginConfiguration stashPluginConfiguration, InputFileCache inputFileCache, FileSystem fileSystem) {
        this.stashPluginConfiguration = stashPluginConfiguration;
        this.inputFileCache = inputFileCache;
        this.fileSystem = fileSystem;
    }

    @Override
    public void analyse(Project module, SensorContext context) {
        for (InputFile inputFile : fileSystem.inputFiles(fileSystem.predicates().all())) {
            inputFileCache.putInputFile(computeEffectiveKey(context.getResource(inputFile), module), inputFile);
        }
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return stashPluginConfiguration.hasToNotifyStash();
    }

    @Override
    public String toString() {
        return "Stash Plugin Inputfile Cache";
    }

    public static String computeEffectiveKey(Resource resource, Project module) {
        return Optional.fromNullable(
                resource.getEffectiveKey()).or(() ->
                module.getKey() + ":" + resource.getKey());
    }
}
