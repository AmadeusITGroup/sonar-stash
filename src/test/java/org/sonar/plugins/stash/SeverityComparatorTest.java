package org.sonar.plugins.stash;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SeverityComparatorTest {

  @Test
  public void testBasic() {
    SeverityComparator c = new SeverityComparator();
    assertEquals(0, c.compare("MAJOR", "MAJOR"));

    assertEquals(-1, c.compare("MINOR", "MAJOR"));
    assertEquals(1, c.compare("MAJOR", "MINOR"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidNoneSeverity() {
    new SeverityComparator().compare("NONE", "MAJOR");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidSeverityEqual() {
    new SeverityComparator().compare("WRONG", "WRONG");
  }

}
