package org.sonar.plugins.stash.issue.collector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.stash.InputFileCache;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputFileCacheTest {

  @Mock
  InputFile inputFile1;

  @Before
  public void setUp() {
    inputFile1 = mock(InputFile.class);
    when(inputFile1.relativePath()).thenReturn("path1");
  }

  @Test
  public void testGetPutInputFile() {
    String componentKey1 = "componentKey1";

    InputFileCache cache = new InputFileCache();
    cache.putInputFile(componentKey1, inputFile1);

    assertEquals(inputFile1, cache.getInputFile("componentKey1"));
    assertEquals(null, cache.getInputFile("componentKey2"));
  }
}
