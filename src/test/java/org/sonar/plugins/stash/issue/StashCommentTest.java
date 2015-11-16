package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mock;

public class StashCommentTest {

  @Mock
  StashUser stashUser = mock(StashUser.class); 
  
  @Test
  public void testGetLine() {
    StashComment comment = new StashComment(1, "message", "path", (long) 123456, stashUser, 0);
    assertEquals(comment.getLine(), 123456);
  }

  @Test
  public void testGetNoLine() {
    StashComment comment = new StashComment(1, "message", "path", null, stashUser, 0);
    assertEquals(comment.getLine(), 0);
  }

}
