package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
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
  
  @Test
  public void testEquals(){
    StashComment comment1 = new StashComment(1, "message", "path", (long) 1, stashUser, 0);
    StashComment comment2 = new StashComment(2, "message", "path", (long) 1, stashUser, 0);
    
    assertFalse(comment1.equals(comment2));
  
    StashComment comment3 = new StashComment(1, "message", "path", (long) 1, stashUser, 0);
    assertTrue(comment1.equals(comment3));
  }
}
