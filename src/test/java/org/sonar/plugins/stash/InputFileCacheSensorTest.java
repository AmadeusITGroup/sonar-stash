package org.sonar.plugins.stash;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

public class InputFileCacheSensorTest {

    @Test
    public void testInputFileCacheSensor_toString() {

        StashPluginConfiguration SPC = mock(StashPluginConfiguration.class);
        InputFileCache IFC = mock(InputFileCache.class);
        FileSystem FS      = mock(FileSystem.class);
        
        InputFileCacheSensor IFCS = new InputFileCacheSensor(SPC, IFC, FS);
        
        Assert.assertEquals("Stash Plugin Inputfile Cache", IFCS.toString());
    }

}