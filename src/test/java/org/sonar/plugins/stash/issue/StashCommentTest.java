package org.sonar.plugins.stash.issue;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StashCommentTest {

  @Mock
  StashUser stashUser = mock(StashUser.class);

  @Test
  public void testGetLine() {
    StashComment comment = new StashComment(1, "message", "path", (long)123456, stashUser, 0);
    assertEquals(123456, comment.getLine());
  }

  @Test
  public void testGetNoLine() {
    StashComment comment = new StashComment(1, "message", "path", null, stashUser, 0);
    assertEquals(0, comment.getLine());
  }

  @Test
  public void testEquals() {
    StashComment comment1 = new StashComment(1, "message", "path", (long)1, stashUser, 0);
    StashComment comment2 = new StashComment(2, "message", "path", (long)1, stashUser, 0);

    assertNotEquals(comment1, comment2);

    StashComment comment3 = new StashComment(1, "message", "path", (long)1, stashUser, 0);
    assertEquals(comment1, comment3);
  }

  @Test
  public void testAddTask() {
    StashTask task1 = mock(StashTask.class);
    when(task1.getId()).thenReturn((long)1111);

    StashTask task2 = mock(StashTask.class);
    when(task2.getId()).thenReturn((long)2222);

    StashComment comment = new StashComment(1, "message", "path", (long)1, stashUser, 0);
    assertEquals(0, comment.getTasks().size());

    comment.addTask(task1);
    assertEquals(1, comment.getTasks().size());
    assertEquals(1111, (long) comment.getTasks().get(0).getId());

    comment.addTask(task2);
    assertEquals(2, comment.getTasks().size());
    assertEquals(1111, (long) comment.getTasks().get(0).getId());
    assertEquals(2222, (long) comment.getTasks().get(1).getId());
  }

  @Test
  public void testContainsNotDeletableTasks() {
    StashComment comment = new StashComment(1, "message", "path", (long)1, stashUser, 0);

    StashTask task1 = mock(StashTask.class);
    when(task1.isDeletable()).thenReturn(true);
    comment.addTask(task1);

    StashTask task2 = mock(StashTask.class);
    when(task2.isDeletable()).thenReturn(true);
    comment.addTask(task2);

    assertFalse(comment.containsPermanentTasks());

    StashTask task3 = mock(StashTask.class);
    when(task3.isDeletable()).thenReturn(false);
    comment.addTask(task3);

    assertTrue(comment.containsPermanentTasks());
  }

  @Test
  public void testContainsNotDeletableTasksWithoutTasks() {
    StashComment comment = new StashComment(1, "message", "path", (long)1, stashUser, 0);
    assertFalse(comment.containsPermanentTasks());
  }

}
