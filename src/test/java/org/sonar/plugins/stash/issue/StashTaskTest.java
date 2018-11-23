package org.sonar.plugins.stash.issue;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StashTaskTest {

  StashTask myTask;

  @BeforeEach
  public void setUp() {
    myTask = new StashTask((long)1111, "Text", "State", true);
  }

  @Test
  public void testGetId() {
    assertEquals(1111, (long)myTask.getId());
  }

  @Test
  public void testGetState() {
    assertEquals("State", myTask.getState());
  }

  @Test
  public void testGetText() {
    assertEquals("Text", myTask.getText());
  }

  @Test
  public void testIsDeletable() {
    assertTrue(myTask.isDeletable());
  }
}
