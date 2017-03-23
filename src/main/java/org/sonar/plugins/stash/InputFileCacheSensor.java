package org.sonar.plugins.stash;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Project;

/*
 * FIXME this should not be necessary, the new plugin API gives us direct access to the InputFile of an issue
 */
public class InputFileCacheSensor implements Sensor, BatchComponent {
    private final InputFileCache inputFileCache;
    private final FileSystem fileSystem;

    public InputFileCacheSensor(InputFileCache inputFileCache, FileSystem fileSystem) {
        this.inputFileCache = inputFileCache;
        this.fileSystem = fileSystem;
    }

    @Override
    public void analyse(Project module, SensorContext context) {
        for (InputFile inputFile : fileSystem.inputFiles(fileSystem.predicates().all())) {
            inputFileCache.putInputFile(context.getResource(inputFile).getEffectiveKey(), inputFile);
        }
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        return true;
    }
}
