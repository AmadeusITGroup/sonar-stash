package org.sonar.plugins.stash.issue;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StashUserTest {

  StashUser myUser;

  @BeforeEach
  public void setUp() {
    myUser = new StashUser(1, "SonarQube", "sonarqube", "sq@email.com");
  }

  @Test
  public void testGetId() {
    assertEquals(1, myUser.getId());
  }

  @Test
  public void testGetName() {
    assertEquals("SonarQube", myUser.getName());
  }

  @Test
  public void testGetSlug() {
    assertEquals("sonarqube", myUser.getSlug());
  }

  @Test
  public void testGetEmail() {
    assertEquals("sq@email.com", myUser.getEmail());
  }


}
