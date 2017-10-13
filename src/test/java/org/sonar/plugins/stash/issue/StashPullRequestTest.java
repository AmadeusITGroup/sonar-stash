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
    assertEquals(myPullRequest.getProject(), "Project");
  }

  @Test
  public void testGetRepository() {
    assertEquals(myPullRequest.getRepository(), "Repository");
  }

  @Test
  public void testGetId() {
    assertEquals(myPullRequest.getId(), 123);
  }

  @Test
  public void testGetVersion() {
    assertEquals(myPullRequest.getVersion(), 0);
    myPullRequest.setVersion(5);
    assertEquals(myPullRequest.getVersion(), 5);
  }

  @Test
  public void testAddReviewer() {
    assertEquals(myPullRequest.getReviewers().size(), 0);

    myPullRequest.addReviewer(stashUser1);

    assertEquals(myPullRequest.getReviewers().size(), 1);
    assertEquals(myPullRequest.getReviewers().get(0).getId(), 1);
  }

  @Test
  public void testAddReviewerTwice() {
    assertEquals(myPullRequest.getReviewers().size(), 0);

    myPullRequest.addReviewer(stashUser1);
    myPullRequest.addReviewer(stashUser2);

    assertEquals(myPullRequest.getReviewers().size(), 2);
    assertEquals(myPullRequest.getReviewers().get(0).getId(), 1);
    assertEquals(myPullRequest.getReviewers().get(1).getId(), 2);
  }

  @Test
  public void testAddReviewerSameTwice() {
    assertEquals(myPullRequest.getReviewers().size(), 0);

    myPullRequest.addReviewer(stashUser1);
    myPullRequest.addReviewer(stashUser1);

    assertEquals(myPullRequest.getReviewers().size(), 2);
    assertEquals(myPullRequest.getReviewers().get(0).getId(), 1);
    assertEquals(myPullRequest.getReviewers().get(1).getId(), 1);
  }

  @Test
  public void testGetReviewer() {
    assertEquals(myPullRequest.getReviewers().size(), 0);
    assertEquals(myPullRequest.getReviewer("sonarqube1"), null);
    assertEquals(myPullRequest.getReviewer("sonarqube2"), null);

    myPullRequest.addReviewer(stashUser1);

    assertEquals(myPullRequest.getReviewers().size(), 1);
    assertEquals(myPullRequest.getReviewer("sonarqube1").getId(), 1);
    assertEquals(myPullRequest.getReviewer("sonarqube2"), null);

    myPullRequest.addReviewer(stashUser2);

    assertEquals(myPullRequest.getReviewers().size(), 2);
    assertEquals(myPullRequest.getReviewer("sonarqube1").getId(), 1);
    assertEquals(myPullRequest.getReviewer("sonarqube2").getId(), 2);
  }

  @Test
  public void testContainsReviewer() {
    assertEquals(myPullRequest.getReviewers().size(), 0);
    assertFalse(myPullRequest.containsReviewer(stashUser1));
    assertFalse(myPullRequest.containsReviewer(stashUser2));

    myPullRequest.addReviewer(stashUser1);

    assertEquals(myPullRequest.getReviewers().size(), 1);
    assertTrue(myPullRequest.containsReviewer(stashUser1));
    assertFalse(myPullRequest.containsReviewer(stashUser2));

    myPullRequest.addReviewer(stashUser2);

    assertEquals(myPullRequest.getReviewers().size(), 2);
    assertTrue(myPullRequest.containsReviewer(stashUser1));
    assertTrue(myPullRequest.containsReviewer(stashUser2));
  }

}