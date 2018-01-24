package org.sonar.plugins.stash.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.plugins.stash.PullRequestRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StashPullRequestTest {

  StashPullRequest myPullRequest;

  StashUser stashUser1;
  StashUser stashUser2;

  @Before
  public void setUp() {
    PullRequestRef pr = PullRequestRef.builder()
                                      .setProject("Project")
                                      .setRepository("Repository")
                                      .setPullRequestId(123)
                                      .build();
    myPullRequest = new StashPullRequest(pr);

    stashUser1 = new StashUser(1, "SonarQube1", "sonarqube1", "sq1@email.com");
    stashUser2 = new StashUser(2, "SonarQube2", "sonarqube2", "sq2@email.com");
  }

  @Test
  public void testGetProject() {
    assertEquals("Project", myPullRequest.getProject());
  }

  @Test
  public void testGetRepository() {
    assertEquals("Repository", myPullRequest.getRepository());
  }

  @Test
  public void testGetId() {
    assertEquals(123, myPullRequest.getId());
  }

  @Test
  public void testGetVersion() {
    assertEquals(0, myPullRequest.getVersion());
    myPullRequest.setVersion(5);
    assertEquals(5, myPullRequest.getVersion());
  }

  @Test
  public void testAddReviewer() {
    assertEquals(0, myPullRequest.getReviewers().size());

    myPullRequest.addReviewer(stashUser1);

    assertEquals(1, myPullRequest.getReviewers().size());
    assertEquals(1, myPullRequest.getReviewers().get(0).getId());
  }

  @Test
  public void testAddReviewerTwice() {
    assertEquals(0, myPullRequest.getReviewers().size());

    myPullRequest.addReviewer(stashUser1);
    myPullRequest.addReviewer(stashUser2);

    assertEquals(2, myPullRequest.getReviewers().size());
    assertEquals(1, myPullRequest.getReviewers().get(0).getId());
    assertEquals(2, myPullRequest.getReviewers().get(1).getId());
  }

  @Test
  public void testAddReviewerSameTwice() {
    assertEquals(myPullRequest.getReviewers().size(), 0);

    myPullRequest.addReviewer(stashUser1);
    myPullRequest.addReviewer(stashUser1);

    assertEquals(2, myPullRequest.getReviewers().size());
    assertEquals(1, myPullRequest.getReviewers().get(0).getId());
    assertEquals(1, myPullRequest.getReviewers().get(1).getId());
  }

  @Test
  public void testGetReviewer() {
    assertEquals(0, myPullRequest.getReviewers().size());
    assertEquals(null, myPullRequest.getReviewer("sonarqube1"));
    assertEquals(null, myPullRequest.getReviewer("sonarqube2"));

    myPullRequest.addReviewer(stashUser1);

    assertEquals(1, myPullRequest.getReviewers().size());
    assertEquals(1, myPullRequest.getReviewer("sonarqube1").getId());
    assertEquals(null, myPullRequest.getReviewer("sonarqube2"));

    myPullRequest.addReviewer(stashUser2);

    assertEquals(2, myPullRequest.getReviewers().size());
    assertEquals(1, myPullRequest.getReviewer("sonarqube1").getId());
    assertEquals(2, myPullRequest.getReviewer("sonarqube2").getId());
  }

  @Test
  public void testContainsReviewer() {
    assertEquals(0, myPullRequest.getReviewers().size());
    assertFalse(myPullRequest.containsReviewer(stashUser1));
    assertFalse(myPullRequest.containsReviewer(stashUser2));

    myPullRequest.addReviewer(stashUser1);

    assertEquals(1, myPullRequest.getReviewers().size());
    assertTrue(myPullRequest.containsReviewer(stashUser1));
    assertFalse(myPullRequest.containsReviewer(stashUser2));

    myPullRequest.addReviewer(stashUser2);

    assertEquals(2, myPullRequest.getReviewers().size());
    assertTrue(myPullRequest.containsReviewer(stashUser1));
    assertTrue(myPullRequest.containsReviewer(stashUser2));
  }

}