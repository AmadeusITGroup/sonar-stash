package org.sonar.plugins.stash.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.StashPlugin.IssueType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StashDiffTest {

  StashDiff diff1;
  StashDiff diff2;
  StashDiff diff3;

  @Before
  public void setUp() {
    StashComment comment1 = mock(StashComment.class);
    when(comment1.getId()).thenReturn((long)12345);

    StashComment comment2 = mock(StashComment.class);
    when(comment2.getId()).thenReturn((long)54321);

    diff1 = new StashDiff(IssueType.CONTEXT, "path/to/diff1", (long)10, (long)20);
    diff1.addComment(comment1);

    diff2 = new StashDiff(IssueType.ADDED, "path/to/diff2", (long)20, (long)30);
    diff2.addComment(comment2);

    diff3 = new StashDiff(IssueType.CONTEXT, "path/to/diff3", (long)30, (long)40);
  }

  @Test
  public void testIsTypeOfContext() {
    assertEquals(diff1.getType(), IssueType.CONTEXT);
    assertNotEquals(diff2.getType(), IssueType.CONTEXT);
  }

  @Test
  public void testContainsComment() {
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));
    assertFalse(diff3.containsComment(12345));
  }

}
