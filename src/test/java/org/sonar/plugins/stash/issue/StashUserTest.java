package org.sonar.plugins.stash.issue;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StashUserTest {

  StashUser myUser;

  @Before
  public void setUp() {
    myUser = new StashUser(1, "SonarQube", "sonarqube", "sq@email.com");
  }

  @Test
  public void testGetId() {
    assertEquals(myUser.getId(), 1);
  }

  @Test
  public void testGetName() {
    assertEquals(myUser.getName(), "SonarQube");
  }

  @Test
  public void testGetSlug() {
    assertEquals(myUser.getSlug(), "sonarqube");
  }

  @Test
  public void testGetEmail() {
    assertEquals(myUser.getEmail(), "sq@email.com");
  }


}
