package org.sonar.plugins.stash.issue;

import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StashCommentTest {

  @Mock
  StashUser stashUser = mock(StashUser.class);

  @Test
  public void testGetLine() {
    StashComment comment = new StashComment(1, "message", "path", (long)123456, stashUser, 0);
    assertEquals(comment.getLine(), 123456);
  }

  @Test
  public void testGetNoLine() {
    StashComment comment = new StashComment(1, "message", "path", null, stashUser, 0);
    assertEquals(comment.getLine(), 0);
  }

  @Test
  public void testEquals() {
    StashComment comment1 = new StashComment(1, "message", "path", (long)1, stashUser, 0);
    StashComment comment2 = new StashComment(2, "message", "path", (long)1, stashUser, 0);

    assertFalse(comment1.equals(comment2));

    StashComment comment3 = new StashComment(1, "message", "path", (long)1, stashUser, 0);
    assertTrue(comment1.equals(comment3));
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
    assertTrue(comment.getTasks().get(0).getId() == 1111);

    comment.addTask(task2);
    assertEquals(2, comment.getTasks().size());
    assertTrue(comment.getTasks().get(0).getId() == 1111);
    assertTrue(comment.getTasks().get(1).getId() == 2222);
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
