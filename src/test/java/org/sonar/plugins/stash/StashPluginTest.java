package org.sonar.plugins.stash;

import org.junit.Test;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class StashPluginTest {

  StashPlugin SPO = new StashPlugin();

  @Test
  public void testGetExtensions() {

    List ext = SPO.getExtensions();

    PropertyDefinition SP_SU = (PropertyDefinition)ext.get(9);
    assertEquals(StashPlugin.STASH_URL, SP_SU.key());

    PropertyDefinition SP_SL = (PropertyDefinition)ext.get(10);
    assertEquals(StashPlugin.STASH_LOGIN, SP_SL.key());

    PropertyDefinition SP_SUS = (PropertyDefinition)ext.get(11);
    assertEquals(StashPlugin.STASH_USER_SLUG, SP_SUS.key());

    PropertyDefinition SP_SPW = (PropertyDefinition)ext.get(12);
    assertEquals(StashPlugin.STASH_PASSWORD, SP_SPW.key());

    PropertyDefinition SP_STO = (PropertyDefinition)ext.get(13);
    assertEquals(StashPlugin.STASH_TIMEOUT, SP_STO.key());

    PropertyDefinition SP_SRA = (PropertyDefinition)ext.get(14);
    assertEquals(StashPlugin.STASH_REVIEWER_APPROVAL, SP_SRA.key());

    PropertyDefinition SP_SIT = (PropertyDefinition)ext.get(15);
    assertEquals(StashPlugin.STASH_ISSUE_THRESHOLD, SP_SIT.key());

    PropertyDefinition SP_RAST = (PropertyDefinition)ext.get(16);
    assertEquals(StashPlugin.STASH_REVIEWER_APPROVAL_SEVERITY_THRESHOLD, SP_RAST.key());

    PropertyDefinition SP_STST = (PropertyDefinition)ext.get(17);
    assertEquals(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD, SP_STST.key());

    PropertyDefinition SP_SIAO = (PropertyDefinition)ext.get(18);
    assertEquals(StashPlugin.STASH_INCLUDE_ANALYSIS_OVERVIEW, SP_SIAO.key());
  }
}