package org.sonar.plugins.stash.issue.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashUser;

public class StashCollectorTest {

  @Test
  public void testExtractSimpleComment() throws Exception {
    int id = 1234;
    String message = "message";
    String path = "path";
    long line = 5;

    String commentString = "{\"values\": [{\"id\":" + id + ", \"text\":\"" + message + "\", \"anchor\": {\"path\":\"" +
        path + "\", \"line\":" + line + "}}]}";
    StashCommentReport commentReport = StashCollector.extractComments(commentString);

    assertEquals(commentReport.size(), 1);
    assertTrue(commentReport.contains(message, path, line));
  }

  @Test
  public void testExtractCommentList() throws Exception {
    int id1 = 1234;
    String message1 = "message1";
    String path1 = "path1";
    long line1 = 1;

    int id2 = 5678;
    String message2 = "message2";
    String path2 = "path2";
    long line2 = 2;
    
    String commentString = "{\"values\": [{\"id\":" + id1 + ",\"text\":\"" + message1 + "\", \"anchor\": {\"path\":\"" + path1 + "\", \"line\":" + line1 + "}}, "
        + "{\"id\":" + id2 + ", \"text\":\"" + message2 + "\", \"anchor\": {\"path\":\"" + path2 + "\", \"line\":" + line2 + "}}]}";
    StashCommentReport commentReport = StashCollector.extractComments(commentString);

    assertEquals(commentReport.size(), 2);
    assertTrue(commentReport.contains(message1, path1, line1));
    assertTrue(commentReport.contains(message2, path2, line2));
  }
  
  @Test
  public void testExtractEmptyComment() throws Exception {
    String commentString = "{\"values\": []}";
    StashCommentReport commentReport = StashCollector.extractComments(commentString);

    assertEquals(commentReport.size(), 0);
  }
  
  @Test
  public void testIsLastPage() throws Exception {
    String jsonBody = "{\"isLastPage\": true}";
    assertTrue(StashCollector.isLastPage(jsonBody));
    
    jsonBody = "{\"isLastPage\": false}";
    assertFalse(StashCollector.isLastPage(jsonBody));
    
    jsonBody = "{\"values\": []}";
    assertTrue(StashCollector.isLastPage(jsonBody));
  }
  
  @Test
  public void testNextPageStart() throws Exception {
    String jsonBody = "{\"nextPageStart\": 3}";
    assertEquals(StashCollector.getNextPageStart(jsonBody), 3);
    
    jsonBody = "{\"values\": []}";
    assertEquals(StashCollector.getNextPageStart(jsonBody), 0);
  }
  
  @Test
  public void testExtractDiffsWithBaseReport() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(DiffReportSample.baseReport);
    assertEquals(report.getDiffs().size(), 4);
    
    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long) 10);
    assertEquals(diff1.getDestination(),(long) 20);
    assertEquals(diff1.getPath(),"stash-plugin/Test.java");
    assertEquals(diff1.getType(),"CONTEXT");
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));
    
    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long) 30);
    assertEquals(diff2.getDestination(),(long) 40);
    assertEquals(diff2.getPath(),"stash-plugin/Test.java");
    assertEquals(diff2.getType(),"ADDED");
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));
    
    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(diff3.getSource(), (long) 40);
    assertEquals(diff3.getDestination(),(long) 50);
    assertEquals(diff3.getPath(),"stash-plugin/Test.java");
    assertEquals(diff3.getType(),"CONTEXT");
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));
    
    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(diff4.getSource(), (long) 60);
    assertEquals(diff4.getDestination(),(long) 70);
    assertEquals(diff4.getPath(),"stash-plugin/Test.java");
    assertEquals(diff4.getType(),"ADDED");
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));
  }
  
  @Test
  public void testExtractDiffsWithEmptyReport() throws Exception {
    String jsonBody = "{ \"diffs\": []}";
    
    StashDiffReport report = StashCollector.extractDiffs(jsonBody);
    assertTrue(report.getDiffs().isEmpty());
    
    report = StashCollector.extractDiffs(DiffReportSample.emptyReport);
    assertTrue(report.getDiffs().isEmpty());
  }
  
  @Test
  public void testExtractDiffsWithMultipleFile() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(DiffReportSample.multipleFileReport);
    assertEquals(report.getDiffs().size(), 2);
    
    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long) 10);
    assertEquals(diff1.getDestination(),(long) 20);
    assertEquals(diff1.getPath(),"stash-plugin/Test.java");
    assertEquals(diff1.getType(),"CONTEXT");
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));
    
    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long) 20);
    assertEquals(diff2.getDestination(),(long) 30);
    assertEquals(diff2.getPath(),"stash-plugin/Test1.java");
    assertEquals(diff2.getType(),"ADDED");
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));
  }
  
  @Test
  public void testExtractDiffsWithDeletedFile() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(DiffReportSample.deletedFileReport);
    assertEquals(report.getDiffs().size(), 2);
    
    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long) 10);
    assertEquals(diff1.getDestination(),(long) 20);
    assertEquals(diff1.getPath(),"stash-plugin/Test2.java");
    assertEquals(diff1.getType(),"CONTEXT");
    
    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long) 30);
    assertEquals(diff2.getDestination(),(long) 40);
    assertEquals(diff2.getPath(),"stash-plugin/Test2.java");
    assertEquals(diff2.getType(),"ADDED");
  }
  
  @Test
  public void testExtractPullRequest() throws Exception {
    String project = "project";
    String repository = "repository";
    String pullRequestId = "123";
    long pullRequestVersion = 1;
    
    long reviewerId = 1;
    String reviewerName = "SonarQube";
    String reviewerSlug = "sonarqube";
    String reviewerEmail = "sq@email.com";
    
    String jsonBody = "{\"id\": " + pullRequestId + ", \"version\": " + pullRequestVersion + ", \"title\": \"PR-Test\","
        + "\"description\": \"PR-test\", \"reviewers\": ["
          + "{\"user\": { \"name\":\"" + reviewerName + "\", \"emailAddress\": \"" + reviewerEmail + "\","
          + "\"id\": " + reviewerId + ", \"slug\": \"" + reviewerSlug + "\"}, \"role\": \"REVIEWER\", \"approved\": false}]}";
    
    StashPullRequest pullRequest = StashCollector.extractPullRequest(project, repository, pullRequestId, jsonBody);
    
    assertEquals(project, pullRequest.getProject());
    assertEquals(repository, pullRequest.getRepository());
    assertEquals(pullRequestId, pullRequest.getId());
    assertEquals(pullRequestVersion, pullRequest.getVersion());
    
    StashUser reviewer = new StashUser(reviewerId, reviewerName, reviewerSlug, reviewerEmail);
    assertEquals(pullRequest.getReviewers().size(), 1);
    assertTrue(pullRequest.containsReviewer(reviewer));
  }
  
  @Test
  public void testExtractPullRequestWithSeveralReviewer() throws Exception {
    String project = "project";
    String repository = "repository";
    String pullRequestId = "123";
    long pullRequestVersion = 1;
    
    long reviewerId1 = 1;
    String reviewerName1 = "SonarQube1";
    String reviewerSlug1 = "sonarqube1";
    String reviewerEmail1 = "sq1@email.com";
    
    long reviewerId2 = 1;
    String reviewerName2 = "SonarQube2";
    String reviewerSlug2 = "sonarqube2";
    String reviewerEmail2 = "sq2@email.com";
    
    String jsonBody = "{\"id\": " + pullRequestId + ", \"version\": " + pullRequestVersion + ", \"title\": \"PR-Test\","
        + "\"description\": \"PR-test\", \"reviewers\": ["
          + "{\"user\": { \"name\":\"" + reviewerName1 + "\", \"emailAddress\": \"" + reviewerEmail1 + "\","
            + "\"id\": " + reviewerId1 + ", \"slug\": \"" + reviewerSlug1 + "\"}, \"role\": \"REVIEWER\", \"approved\": false},"
          + "{\"user\": { \"name\":\"" + reviewerName2 + "\", \"emailAddress\": \"" + reviewerEmail2 + "\","
            + "\"id\": " + reviewerId2 + ", \"slug\": \"" + reviewerSlug2 + "\"}, \"role\": \"REVIEWER\", \"approved\": false}]}";
    
    StashPullRequest pullRequest = StashCollector.extractPullRequest(project, repository, pullRequestId, jsonBody);
    
    assertEquals(project, pullRequest.getProject());
    assertEquals(repository, pullRequest.getRepository());
    assertEquals(pullRequestId, pullRequest.getId());
    assertEquals(pullRequestVersion, pullRequest.getVersion());
    
    StashUser reviewer1 = new StashUser(reviewerId1, reviewerName1, reviewerSlug1, reviewerEmail1);
    StashUser reviewer2 = new StashUser(reviewerId2, reviewerName2, reviewerSlug2, reviewerEmail2);
    assertEquals(pullRequest.getReviewers().size(), 2);
    assertTrue(pullRequest.containsReviewer(reviewer1));
    assertTrue(pullRequest.containsReviewer(reviewer2));
  }
  
  @Test
  public void testExtractPullRequestWithNoReviewer() throws Exception {
    String project = "project";
    String repository = "repository";
    String pullRequestId = "123";
    long pullRequestVersion = 1;
    
    String jsonBody = "{\"id\": " + pullRequestId + ", \"version\": " + pullRequestVersion + ", \"title\": \"PR-Test\","
        + "\"description\": \"PR-test\", \"reviewers\": []}";
    
    StashPullRequest pullRequest = StashCollector.extractPullRequest(project, repository, pullRequestId, jsonBody);
    
    assertEquals(project, pullRequest.getProject());
    assertEquals(repository, pullRequest.getRepository());
    assertEquals(pullRequestId, pullRequest.getId());
    assertEquals(pullRequestVersion, pullRequest.getVersion());
    
    assertEquals(pullRequest.getReviewers().size(), 0);
  }
  
  @Test
  public void testExtractUser() throws Exception {
    long userId = 1;
    String userName = "SonarQube";
    String userSlug = "sonarqube";
    String userEmail = "sq@email.com";
    
    String jsonBody = "{ \"name\":\"" + userName + "\", \"emailAddress\": \"" + userEmail + "\","
        + "\"id\": " + userId + ", \"slug\": \"" + userSlug + "\"}";
    
    StashUser user = StashCollector.extractUser(jsonBody);
    assertEquals(user.getId(), userId);
    assertEquals(user.getName(), userName);
    assertEquals(user.getSlug(), userSlug);
    assertEquals(user.getEmail(), userEmail);
  }
}
