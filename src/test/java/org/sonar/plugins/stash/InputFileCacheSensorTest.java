package org.sonar.plugins.stash;

import org.junit.Assert;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class InputFileCacheSensorTest {

  private StashPluginConfiguration SPC = mock(StashPluginConfiguration.class);
  private InputFileCache IFC = mock(InputFileCache.class);
  private FileSystem FS = mock(FileSystem.class);

  private InputFileCacheSensor IFCS = new InputFileCacheSensor(SPC, IFC, FS);


  @Test
  public void testInputFileCacheSensor_toString() {

    Assert.assertEquals("Stash Plugin Inputfile Cache", IFCS.toString());
  }

  @Test
  public void testShouldExecuteOnProject() {

    assertFalse(IFCS.shouldExecuteOnProject(null));
  }

  @Test
  public void testComputeEffectiveKeyEither() {

    Resource rsrc = mock(Resource.class);
    Project proj = mock(Project.class);

    when(rsrc.getEffectiveKey()).thenReturn("YOLO");

    assertEquals("YOLO", IFCS.computeEffectiveKey(rsrc, proj));
  }

  @Test
  public void testAnalyse() {

    Project proj = mock(Project.class);
    SensorContext SeCo = mock(SensorContext.class);

    // We need to create an iterable of InputFile to mock the search for files
    DefaultInputFile DIF = new DefaultInputFile("nowhere");
    List<? extends InputFile> listOfFiles = Collections.singletonList(DIF);

    // Then we can mock the fileSystem object with the mocked list
    FileSystem fileSystemPlus = mock(FileSystem.class);
    doReturn(listOfFiles).when(fileSystemPlus).inputFiles(any());

    // And also make sure that the predictate() call does not return null :(
    FilePredicates predicates = mock(FilePredicates.class);
    when(fileSystemPlus.predicates()).thenReturn(predicates);

    // We use a real object here to save time on mocking
    InputFileCache IFC_real = new InputFileCache();


    // We need a Resource for the SensorContext mock
    Resource rsrc = mock(Resource.class);
    when(rsrc.getEffectiveKey()).thenReturn("YOLO");

    // The SensorContext also requires some mock to run everything smoothly
    doReturn(rsrc).when(SeCo).getResource(any(InputPath.class));

    // And finally, we can construct the test object to run the method we want to test
    InputFileCacheSensor IFCS_plus = new InputFileCacheSensor(SPC, IFC_real, fileSystemPlus);

    IFCS_plus.analyse(proj, SeCo);
  }
}