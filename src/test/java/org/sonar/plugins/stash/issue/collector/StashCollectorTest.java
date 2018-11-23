package org.sonar.plugins.stash.issue.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.StashPlugin.IssueType;
import org.sonar.plugins.stash.exceptions.StashReportExtractionException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

public class StashCollectorTest {

  private static final long STASH_USER_ID = 1;
  PullRequestRef pr = PullRequestRef.builder()
                                    .setProject("project")
                                    .setRepository("repository")
                                    .setPullRequestId(123)
                                    .build();

  @Test
  public void testExtractCommentReport() throws Exception {
    String commentString =
        "{\"values\": [{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}]}";
    StashCommentReport commentReport = StashCollector.extractComments(parse(commentString));

    assertEquals(1, commentReport.size());

    StashComment comment = commentReport.getComments().get(0);
    assertEquals(1234, comment.getId());
    assertEquals("message", comment.getMessage());
    assertEquals("path", comment.getPath());
    assertEquals(0, comment.getVersion());
    assertEquals(STASH_USER_ID, comment.getAuthor().getId());
    assertEquals(5, comment.getLine());
  }

  @Test
  public void testExtractCommentReportWithSeveralComment() throws Exception {
    String commentString = "{\"values\": ["
                           + "{\"id\":1234, \"text\":\"message1\", \"anchor\": {\"path\":\"path1\", \"line\":1},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":1}, "
                           + "{\"id\":5678, \"text\":\"message2\", \"anchor\": {\"path\":\"path2\", \"line\":2},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":2}]}";

    StashCommentReport commentReport = StashCollector.extractComments(parse(commentString));

    assertEquals(2, commentReport.size());

    StashComment comment1 = commentReport.getComments().get(0);
    assertEquals(1234, comment1.getId());
    assertEquals("message1", comment1.getMessage());
    assertEquals("path1", comment1.getPath());
    assertEquals(1, comment1.getVersion());
    assertEquals(STASH_USER_ID, comment1.getAuthor().getId());
    assertEquals(1, comment1.getLine());

    StashComment comment2 = commentReport.getComments().get(1);
    assertEquals(5678, comment2.getId());
    assertEquals("message2", comment2.getMessage());
    assertEquals("path2", comment2.getPath());
    assertEquals(2, comment2.getVersion());
    assertEquals(STASH_USER_ID, comment2.getAuthor().getId());
    assertEquals(2, comment2.getLine());
  }

  @Test
  public void testExtractEmptyCommentReport() throws Exception {
    String commentString = "{\"values\": []}";
    StashCommentReport commentReport = StashCollector.extractComments(parse(commentString));

    assertEquals(0, commentReport.size());
  }

  @Test
  public void testExtractComment() throws Exception {
    String commentString = "{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}";

    StashComment comment = StashCollector.extractComment(parse(commentString));

    assertEquals(1234, comment.getId());
    assertEquals("message", comment.getMessage());
    assertEquals("path", comment.getPath());
    assertEquals(0, comment.getVersion());
    assertEquals(STASH_USER_ID, comment.getAuthor().getId());
    assertEquals(5, comment.getLine());
  }

  @Test
  public void testExtractEmptyCommentWithNoAnchor() throws Exception {
    String commentString = "{\"id\":1234, \"text\":\"message\", "
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}";

    assertThrows(StashReportExtractionException.class, () ->
        StashCollector.extractComment(parse(commentString))
    );
  }

  @Test
  public void testExtractCommentWithPathAndLineAsParameters() throws Exception {
    String commentString = "{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}";

    StashComment comment = StashCollector.extractComment(parse(commentString), "pathAsParameter", (long)1111);

    assertEquals(1234, comment.getId());
    assertEquals("message", comment.getMessage());
    assertEquals("pathAsParameter", comment.getPath());
    assertEquals(0, comment.getVersion());
    assertEquals(STASH_USER_ID, comment.getAuthor().getId());
    assertEquals(1111, comment.getLine());
  }

  @Test
  public void testIsLastPage() throws Exception {
    String jsonBody = "{\"isLastPage\": true}";
    assertTrue(StashCollector.isLastPage(parse(jsonBody)));

    jsonBody = "{\"isLastPage\": false}";
    assertFalse(StashCollector.isLastPage(parse(jsonBody)));

    jsonBody = "{\"values\": []}";
    assertTrue(StashCollector.isLastPage(parse(jsonBody)));
  }

  @Test
  public void testNextPageStart() throws Exception {
    String jsonBody = "{\"nextPageStart\": 3}";
    assertEquals(3, StashCollector.getNextPageStart(parse(jsonBody)));

    jsonBody = "{\"values\": []}";
    assertEquals(0, StashCollector.getNextPageStart(parse(jsonBody)));
  }

  @Test
  public void testExtractDiffsWithBaseReport() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReport));
    assertEquals(4, report.getDiffs().size());

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals((long)10, diff1.getSource());
    assertEquals((long)20, diff1.getDestination());
    assertEquals("stash-plugin/Test.java", diff1.getPath());
    assertEquals(IssueType.CONTEXT, diff1.getType());
    assertEquals(1, diff1.getComments().size());
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashComment comment1 = diff1.getComments().get(0);
    assertEquals(12345, comment1.getId());
    assertEquals("Test comment", comment1.getMessage());
    assertEquals(1, comment1.getVersion());

    StashTask task1 = comment1.getTasks().get(0);
    assertEquals(12345, (long)task1.getId());
    assertEquals("Complete the task associated to this TODO comment.", task1.getText());
    assertEquals("OPENED", task1.getState());

    StashTask task2 = comment1.getTasks().get(1);
    assertEquals(54321, (long)task2.getId());
    assertEquals("Complete the task associated to this TODO comment.", task2.getText());
    assertEquals("OPENED", task2.getState());

    StashUser author1 = comment1.getAuthor();
    assertEquals(12345, author1.getId());
    assertEquals("SonarQube", author1.getName());
    assertEquals("sonarqube", author1.getSlug());
    assertEquals("sq@email.com", author1.getEmail());

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals((long)30, diff2.getSource());
    assertEquals((long)40, diff2.getDestination());
    assertEquals("stash-plugin/Test.java", diff2.getPath());
    assertEquals(IssueType.ADDED, diff2.getType());
    assertEquals(0, diff2.getComments().size());
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals((long)40, diff3.getSource());
    assertEquals((long)50, diff3.getDestination());
    assertEquals("stash-plugin/Test.java", diff3.getPath());
    assertEquals(IssueType.CONTEXT, diff3.getType());
    assertEquals(1, diff3.getComments().size());
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));

    StashComment comment2 = diff3.getComments().get(0);
    assertEquals(54321, comment2.getId());
    assertEquals("Test comment 2", comment2.getMessage());
    assertEquals(1, comment2.getVersion());

    StashUser author2 = comment2.getAuthor();
    assertEquals(54321, author2.getId());
    assertEquals("SonarQube2", author2.getName());
    assertEquals("sonarqube2", author2.getSlug());
    assertEquals("sq2@email.com", author2.getEmail());

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals((long)60, diff4.getSource());
    assertEquals((long)70, diff4.getDestination());
    assertEquals("stash-plugin/Test.java", diff4.getPath());
    assertEquals(IssueType.ADDED, diff4.getType());
    assertEquals(0, diff4.getComments().size());
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));
  }

  @Test
  public void testExtractDiffsWithNoComments() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReportWithNoComments));
    assertEquals(4, report.getDiffs().size());

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals((long)10, diff1.getSource());
    assertEquals((long)20, diff1.getDestination());
    assertEquals("stash-plugin/Test.java", diff1.getPath());
    assertEquals(IssueType.CONTEXT, diff1.getType());
    assertEquals(0, diff1.getComments().size());

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals((long)30, diff2.getSource());
    assertEquals((long)40, diff2.getDestination());
    assertEquals("stash-plugin/Test.java", diff2.getPath());
    assertEquals(IssueType.ADDED, diff2.getType());
    assertEquals(0, diff2.getComments().size());

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals((long)40, diff3.getSource());
    assertEquals((long)50, diff3.getDestination());
    assertEquals("stash-plugin/Test.java", diff3.getPath());
    assertEquals(IssueType.CONTEXT, diff3.getType());
    assertEquals(0, diff3.getComments().size());

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals((long)60, diff4.getSource());
    assertEquals((long)70, diff4.getDestination());
    assertEquals("stash-plugin/Test.java", diff4.getPath());
    assertEquals(IssueType.ADDED, diff4.getType());
    assertEquals(0, diff4.getComments().size());
  }

  @Test
  public void testExtractDiffsWithFileComments() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReportWithFileComments));
    assertEquals(5, report.getDiffs().size());

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(1, diff1.getComments().size());
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashComment comment1 = diff1.getComments().get(0);
    assertEquals(12345, comment1.getId());
    assertEquals("Test comment", comment1.getMessage());
    assertEquals(1, comment1.getVersion());

    StashUser author1 = comment1.getAuthor();
    assertEquals(12345, author1.getId());
    assertEquals("SonarQube", author1.getName());
    assertEquals("sonarqube", author1.getSlug());
    assertEquals("sq@email.com", author1.getEmail());

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(0, diff2.getComments().size());
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(1, diff3.getComments().size());
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));

    StashComment comment2 = diff3.getComments().get(0);
    assertEquals(54321, comment2.getId());
    assertEquals("Test comment 2", comment2.getMessage());
    assertEquals(1, comment2.getVersion());

    StashUser author2 = comment2.getAuthor();
    assertEquals(54321, author2.getId());
    assertEquals("SonarQube2", author2.getName());
    assertEquals("sonarqube2", author2.getSlug());
    assertEquals("sq2@email.com", author2.getEmail());

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(0, diff4.getComments().size());
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));

    StashDiff diff5 = report.getDiffs().get(4);
    assertEquals(0, diff5.getSource());
    assertEquals(0, diff5.getDestination());
    assertEquals("stash-plugin/Test.java", diff5.getPath());
    assertEquals(IssueType.CONTEXT, diff5.getType());
    assertEquals(2, diff5.getComments().size());
    assertFalse(diff5.containsComment(12345));
    assertFalse(diff5.containsComment(54321));
    assertTrue(diff5.containsComment(123456));
    assertTrue(diff5.containsComment(654321));

    StashComment comment3 = diff5.getComments().get(0);
    assertEquals(123456, comment3.getId());
    assertEquals(1, comment3.getVersion());
    assertEquals("Test File comment", comment3.getMessage());

    StashUser author3 = comment3.getAuthor();
    assertEquals(12345, author3.getId());
    assertEquals("SonarQube", author3.getName());
    assertEquals("sonarqube", author3.getSlug());
    assertEquals("sq@email.com", author3.getEmail());

    StashComment comment4 = diff5.getComments().get(1);
    assertEquals(654321, comment4.getId());
    assertEquals("Test File comment 2", comment4.getMessage());
    assertEquals(1, comment4.getVersion());

    StashUser author4 = comment4.getAuthor();
    assertEquals(54321, author4.getId());
    assertEquals("SonarQube2", author4.getName());
    assertEquals("sonarqube2", author4.getSlug());
    assertEquals("sq2@email.com", author4.getEmail());
  }

  @Test
  public void testExtractDiffsWithEmptyFileComments() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReportWithEmptyFileComments));
    assertEquals(5, report.getDiffs().size());

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(1, diff1.getComments().size());
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashComment comment1 = diff1.getComments().get(0);
    assertEquals(12345, comment1.getId());
    assertEquals("Test comment", comment1.getMessage());
    assertEquals(1, comment1.getVersion());

    StashUser author1 = comment1.getAuthor();
    assertEquals(12345, author1.getId());
    assertEquals("SonarQube", author1.getName());
    assertEquals("sonarqube", author1.getSlug());
    assertEquals("sq@email.com", author1.getEmail());

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(0, diff2.getComments().size());
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(1, diff3.getComments().size());
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(0, diff4.getComments().size());
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));

    StashDiff diff5 = report.getDiffs().get(4);
    assertEquals(0, diff5.getSource());
    assertEquals(0, diff5.getDestination());
    assertEquals("stash-plugin/Test.java", diff5.getPath());
    assertEquals(IssueType.CONTEXT, diff5.getType());
    assertEquals(0, diff5.getComments().size());
    assertFalse(diff5.containsComment(12345));
    assertFalse(diff5.containsComment(54321));
  }

  @Test
  public void testExtractDiffsWithEmptyReport() throws Exception {
    String jsonBody = "{ \"diffs\": []}";

    StashDiffReport report = StashCollector.extractDiffs(parse(jsonBody));
    assertTrue(report.getDiffs().isEmpty());

    report = StashCollector.extractDiffs(parse(DiffReportSample.emptyReport));
    assertTrue(report.getDiffs().isEmpty());
  }

  @Test
  public void testExtractDiffsWithMultipleFile() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.multipleFileReport));
    assertEquals(2, report.getDiffs().size());

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals((long)10, diff1.getSource());
    assertEquals((long)20, diff1.getDestination());
    assertEquals("stash-plugin/Test.java", diff1.getPath());
    assertEquals(IssueType.CONTEXT, diff1.getType());
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals((long)20, diff2.getSource());
    assertEquals((long)30, diff2.getDestination());
    assertEquals("stash-plugin/Test1.java", diff2.getPath());
    assertEquals(IssueType.ADDED, diff2.getType());
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));
  }

  @Test
  public void testExtractDiffsWithDeletedFile() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.deletedFileReport));
    assertEquals(2, report.getDiffs().size());

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals((long)10, diff1.getSource());
    assertEquals((long)20, diff1.getDestination());
    assertEquals("stash-plugin/Test2.java", diff1.getPath());
    assertEquals(IssueType.CONTEXT, diff1.getType());

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals((long)30, diff2.getSource());
    assertEquals((long)40, diff2.getDestination());
    assertEquals("stash-plugin/Test2.java", diff2.getPath());
    assertEquals(IssueType.ADDED, diff2.getType());
  }

  @Test
  public void testExtractPullRequest() throws Exception {
    String project = "project";
    String repository = "repository";
    int pullRequestId = 123;
    long pullRequestVersion = 1;

    long reviewerId = 1;
    String reviewerName = "SonarQube";
    String reviewerSlug = "sonarqube";
    String reviewerEmail = "sq@email.com";

    String jsonBody = "{\"id\": "
                      + pullRequestId
                      + ", \"version\": "
                      + pullRequestVersion
                      + ", \"title\": \"PR-Test\","
                      + "\"description\": \"PR-test\", \"reviewers\": ["
                      + "{\"user\": { \"name\":\""
                      + reviewerName
                      + "\", \"emailAddress\": \""
                      + reviewerEmail
                      + "\","
                      + "\"id\": "
                      + reviewerId
                      + ", \"slug\": \""
                      + reviewerSlug
                      + "\"}, \"role\": \"REVIEWER\", \"approved\": false}]}";

    StashPullRequest pullRequest = StashCollector.extractPullRequest(pr, parse(jsonBody));

    assertEquals(project, pullRequest.getProject());
    assertEquals(repository, pullRequest.getRepository());
    assertEquals(pullRequestId, pullRequest.getId());
    assertEquals(pullRequestVersion, pullRequest.getVersion());

    StashUser reviewer = new StashUser(reviewerId, reviewerName, reviewerSlug, reviewerEmail);
    assertEquals(1, pullRequest.getReviewers().size());
    assertTrue(pullRequest.containsReviewer(reviewer));
  }

  @Test
  public void testExtractPullRequestWithSeveralReviewer() throws Exception {
    String project = "project";
    String repository = "repository";
    int pullRequestId = 123;
    long pullRequestVersion = 1;

    long reviewerId1 = 1;
    String reviewerName1 = "SonarQube1";
    String reviewerSlug1 = "sonarqube1";
    String reviewerEmail1 = "sq1@email.com";

    long reviewerId2 = 1;
    String reviewerName2 = "SonarQube2";
    String reviewerSlug2 = "sonarqube2";
    String reviewerEmail2 = "sq2@email.com";

    String jsonBody = "{\"id\": "
                      + pullRequestId
                      + ", \"version\": "
                      + pullRequestVersion
                      + ", \"title\": \"PR-Test\","
                      + "\"description\": \"PR-test\", \"reviewers\": ["
                      + "{\"user\": { \"name\":\""
                      + reviewerName1
                      + "\", \"emailAddress\": \""
                      + reviewerEmail1
                      + "\","
                      + "\"id\": "
                      + reviewerId1
                      + ", \"slug\": \""
                      + reviewerSlug1
                      + "\"}, \"role\": \"REVIEWER\", \"approved\": false},"
                      + "{\"user\": { \"name\":\""
                      + reviewerName2
                      + "\", \"emailAddress\": \""
                      + reviewerEmail2
                      + "\","
                      + "\"id\": "
                      + reviewerId2
                      + ", \"slug\": \""
                      + reviewerSlug2
                      + "\"}, \"role\": \"REVIEWER\", \"approved\": false}]}";

    StashPullRequest pullRequest = StashCollector.extractPullRequest(pr, parse(jsonBody));

    assertEquals(project, pullRequest.getProject());
    assertEquals(repository, pullRequest.getRepository());
    assertEquals(pullRequestId, pullRequest.getId());
    assertEquals(pullRequestVersion, pullRequest.getVersion());

    StashUser reviewer1 = new StashUser(reviewerId1, reviewerName1, reviewerSlug1, reviewerEmail1);
    StashUser reviewer2 = new StashUser(reviewerId2, reviewerName2, reviewerSlug2, reviewerEmail2);
    assertEquals(2, pullRequest.getReviewers().size());
    assertTrue(pullRequest.containsReviewer(reviewer1));
    assertTrue(pullRequest.containsReviewer(reviewer2));
  }

  @Test
  public void testExtractPullRequestWithNoReviewer() throws Exception {
    String project = "project";
    String repository = "repository";
    int pullRequestId = 123;
    long pullRequestVersion = 1;

    String jsonBody = "{\"id\": " + pullRequestId + ", \"version\": " + pullRequestVersion + ", \"title\": \"PR-Test\","
                      + "\"description\": \"PR-test\", \"reviewers\": []}";

    StashPullRequest pullRequest = StashCollector.extractPullRequest(pr, parse(jsonBody));

    assertEquals(project, pullRequest.getProject());
    assertEquals(repository, pullRequest.getRepository());
    assertEquals(pullRequestId, pullRequest.getId());
    assertEquals(pullRequestVersion, pullRequest.getVersion());

    assertEquals(0, pullRequest.getReviewers().size());
  }

  @Test
  public void testExtractUser() throws Exception {
    long userId = 1;
    String userName = "SonarQube";
    String userSlug = "sonarqube";
    String userEmail = "sq@email.com";

    String jsonBody = "{ \"name\":\"" + userName + "\", \"email\": \"" + userEmail + "\","
                      + "\"id\": " + userId + ", \"slug\": \"" + userSlug + "\"}";

    StashUser user = StashCollector.extractUser(parse(jsonBody));
    assertEquals(userId, user.getId());
    assertEquals(userName, user.getName());
    assertEquals(userSlug, user.getSlug());
    assertEquals(userEmail, user.getEmail());
  }

  @Test
  public void testExtractTask() throws Exception {
    long id = 1111;
    String text = "Text";
    String state = "State";
    boolean deletable = true;

    String jsonTask = "{ \"id\":" + id + ", \"text\":\"" + text + "\", \"state\":\"" + state + "\","
                      + "\"permittedOperations\": { \"deletable\":" + deletable + "}}";

    StashTask task = StashCollector.extractTask(parse(jsonTask));

    assertEquals(id, (long)task.getId());
    assertEquals(text, task.getText());
    assertEquals(state, task.getState());
    assertEquals(deletable, task.isDeletable());
  }

  @Test
  public void testExtractTaskWithoutPermittedOperation() throws Exception {
    long id = 1111;
    String text = "Text";
    String state = "State";

    String jsonTask = "{ \"id\":" + id + ", \"text\":\"" + text + "\", \"state\": \"" + state + "\"}";

    StashTask task = StashCollector.extractTask(parse(jsonTask));

    assertEquals(id, (long)task.getId());
    assertEquals(text, task.getText());
    assertEquals(state, task.getState());
    assertTrue(task.isDeletable());
  }

  private static JsonObject parse(String s) throws Exception {
    return (JsonObject) Jsoner.deserialize(s);
  }
}
