package org.sonar.plugins.stash.issue;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StashCommentTest {

  @Test
  public void testGetLine() {
    StashComment comment = new StashComment(1, "message", "path", new Long(123456));
    assertEquals(comment.getLine(), 123456);
  }

  @Test
  public void testGetNoLine() {
    StashComment comment = new StashComment(1, "message", "path", null);
    assertEquals(comment.getLine(), 0);
  }

}
