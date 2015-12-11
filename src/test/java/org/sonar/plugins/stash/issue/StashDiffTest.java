package org.sonar.plugins.stash.issue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.stash.StashPlugin;

public class StashDiffTest {
  
  StashDiff diff1;
  StashDiff diff2;
  StashDiff diff3;

  @Before
  public void setUp(){
    diff1 = new StashDiff(StashPlugin.CONTEXT_ISSUE_TYPE, "path/to/diff1", (long) 10, (long) 20);
    diff1.addComment(12345);
    
    diff2 = new StashDiff(StashPlugin.ADDED_ISSUE_TYPE, "path/to/diff2", (long) 20, (long) 30);
    diff2.addComment(54321);
    
    diff3 = new StashDiff(StashPlugin.CONTEXT_ISSUE_TYPE, "path/to/diff3", (long) 30, (long) 40);
  }
  
  @Test
  public void testIsTypeOfContext(){
    assertTrue(diff1.isTypeOfContext());
    assertFalse(diff2.isTypeOfContext());
  }
  
  @Test
  public void testContainsComment(){
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));
    assertFalse(diff2.containsComment(12345));
  }

}
