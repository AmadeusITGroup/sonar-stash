package org.sonar.plugins.stash.issue.collector;

import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.Test;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

    assertEquals(commentReport.size(), 1);

    StashComment comment = commentReport.getComments().get(0);
    assertEquals(comment.getId(), 1234);
    assertEquals(comment.getMessage(), "message");
    assertEquals(comment.getPath(), "path");
    assertEquals(comment.getVersion(), 0);
    assertEquals(comment.getAuthor().getId(), STASH_USER_ID);
    assertEquals(comment.getLine(), 5);
  }

  @Test
  public void testExtractCommentReportWithSeveralComment() throws Exception {
    String commentString = "{\"values\": ["
                           + "{\"id\":1234, \"text\":\"message1\", \"anchor\": {\"path\":\"path1\", \"line\":1},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":1}, "
                           + "{\"id\":5678, \"text\":\"message2\", \"anchor\": {\"path\":\"path2\", \"line\":2},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":2}]}";

    StashCommentReport commentReport = StashCollector.extractComments(parse(commentString));

    assertEquals(commentReport.size(), 2);

    StashComment comment1 = commentReport.getComments().get(0);
    assertEquals(comment1.getId(), 1234);
    assertEquals(comment1.getMessage(), "message1");
    assertEquals(comment1.getPath(), "path1");
    assertEquals(comment1.getVersion(), 1);
    assertEquals(comment1.getAuthor().getId(), STASH_USER_ID);
    assertEquals(comment1.getLine(), 1);

    StashComment comment2 = commentReport.getComments().get(1);
    assertEquals(comment2.getId(), 5678);
    assertEquals(comment2.getMessage(), "message2");
    assertEquals(comment2.getPath(), "path2");
    assertEquals(comment2.getVersion(), 2);
    assertEquals(comment2.getAuthor().getId(), STASH_USER_ID);
    assertEquals(comment2.getLine(), 2);
  }

  @Test
  public void testExtractEmptyCommentReport() throws Exception {
    String commentString = "{\"values\": []}";
    StashCommentReport commentReport = StashCollector.extractComments(parse(commentString));

    assertEquals(commentReport.size(), 0);
  }

  @Test
  public void testExtractComment() throws Exception {
    String commentString = "{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}";

    StashComment comment = StashCollector.extractComment(parse(commentString));

    assertEquals(comment.getId(), 1234);
    assertEquals(comment.getMessage(), "message");
    assertEquals(comment.getPath(), "path");
    assertEquals(comment.getVersion(), 0);
    assertEquals(comment.getAuthor().getId(), STASH_USER_ID);
    assertEquals(comment.getLine(), 5);
  }

  @Test
  public void testExtractEmptyCommentWithNoAnchor() throws Exception {
    String commentString = "{\"id\":1234, \"text\":\"message\", "
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}";

    try {
      StashCollector.extractComment(parse(commentString));

      assertFalse("No anchor tag: extraction should raised StashReportExtractionException exception", true);

    } catch (StashReportExtractionException e) {
      assertTrue("No anchor tag: extraction has raised StashReportExtractionException exception as expected", true);
    }
  }

  @Test
  public void testExtractCommentWithPathAndLineAsParameters() throws Exception {
    String commentString = "{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
                           + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}";

    StashComment comment = StashCollector.extractComment(parse(commentString), "pathAsParameter", (long)1111);

    assertEquals(comment.getId(), 1234);
    assertEquals(comment.getMessage(), "message");
    assertEquals(comment.getPath(), "pathAsParameter");
    assertEquals(comment.getVersion(), 0);
    assertEquals(comment.getAuthor().getId(), STASH_USER_ID);
    assertEquals(comment.getLine(), 1111);
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
    assertEquals(StashCollector.getNextPageStart(parse(jsonBody)), 3);

    jsonBody = "{\"values\": []}";
    assertEquals(StashCollector.getNextPageStart(parse(jsonBody)), 0);
  }

  @Test
  public void testExtractDiffsWithBaseReport() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReport));
    assertEquals(report.getDiffs().size(), 4);

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long)10);
    assertEquals(diff1.getDestination(), (long)20);
    assertEquals(diff1.getPath(), "stash-plugin/Test.java");
    assertEquals(diff1.getType(), IssueType.CONTEXT);
    assertEquals(diff1.getComments().size(), 1);
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashComment comment1 = diff1.getComments().get(0);
    assertEquals(comment1.getId(), 12345);
    assertEquals(comment1.getMessage(), "Test comment");
    assertEquals(comment1.getVersion(), 1);

    StashTask task1 = comment1.getTasks().get(0);
    assertEquals(12345, (long)task1.getId());
    assertEquals("Complete the task associated to this TODO comment.", task1.getText());
    assertEquals("OPENED", task1.getState());

    StashTask task2 = comment1.getTasks().get(1);
    assertEquals(54321, (long)task2.getId());
    assertEquals("Complete the task associated to this TODO comment.", task2.getText());
    assertEquals("OPENED", task2.getState());

    StashUser author1 = comment1.getAuthor();
    assertEquals(author1.getId(), 12345);
    assertEquals(author1.getName(), "SonarQube");
    assertEquals(author1.getSlug(), "sonarqube");
    assertEquals(author1.getEmail(), "sq@email.com");

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long)30);
    assertEquals(diff2.getDestination(), (long)40);
    assertEquals(diff2.getPath(), "stash-plugin/Test.java");
    assertEquals(diff2.getType(), IssueType.ADDED);
    assertEquals(diff2.getComments().size(), 0);
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(diff3.getSource(), (long)40);
    assertEquals(diff3.getDestination(), (long)50);
    assertEquals(diff3.getPath(), "stash-plugin/Test.java");
    assertEquals(diff3.getType(), IssueType.CONTEXT);
    assertEquals(diff3.getComments().size(), 1);
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));

    StashComment comment2 = diff3.getComments().get(0);
    assertEquals(comment2.getId(), 54321);
    assertEquals(comment2.getMessage(), "Test comment 2");
    assertEquals(comment2.getVersion(), 1);

    StashUser author2 = comment2.getAuthor();
    assertEquals(author2.getId(), 54321);
    assertEquals(author2.getName(), "SonarQube2");
    assertEquals(author2.getSlug(), "sonarqube2");
    assertEquals(author2.getEmail(), "sq2@email.com");

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(diff4.getSource(), (long)60);
    assertEquals(diff4.getDestination(), (long)70);
    assertEquals(diff4.getPath(), "stash-plugin/Test.java");
    assertEquals(diff4.getType(), IssueType.ADDED);
    assertEquals(diff4.getComments().size(), 0);
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));
  }

  @Test
  public void testExtractDiffsWithNoComments() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReportWithNoComments));
    assertEquals(report.getDiffs().size(), 4);

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long)10);
    assertEquals(diff1.getDestination(), (long)20);
    assertEquals(diff1.getPath(), "stash-plugin/Test.java");
    assertEquals(diff1.getType(), IssueType.CONTEXT);
    assertEquals(diff1.getComments().size(), 0);

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long)30);
    assertEquals(diff2.getDestination(), (long)40);
    assertEquals(diff2.getPath(), "stash-plugin/Test.java");
    assertEquals(diff2.getType(), IssueType.ADDED);
    assertEquals(diff2.getComments().size(), 0);

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(diff3.getSource(), (long)40);
    assertEquals(diff3.getDestination(), (long)50);
    assertEquals(diff3.getPath(), "stash-plugin/Test.java");
    assertEquals(diff3.getType(), IssueType.CONTEXT);
    assertEquals(diff3.getComments().size(), 0);

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(diff4.getSource(), (long)60);
    assertEquals(diff4.getDestination(), (long)70);
    assertEquals(diff4.getPath(), "stash-plugin/Test.java");
    assertEquals(diff4.getType(), IssueType.ADDED);
    assertEquals(diff4.getComments().size(), 0);
  }

  @Test
  public void testExtractDiffsWithFileComments() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReportWithFileComments));
    assertEquals(report.getDiffs().size(), 5);

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getComments().size(), 1);
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashComment comment1 = diff1.getComments().get(0);
    assertEquals(comment1.getId(), 12345);
    assertEquals(comment1.getMessage(), "Test comment");
    assertEquals(comment1.getVersion(), 1);

    StashUser author1 = comment1.getAuthor();
    assertEquals(author1.getId(), 12345);
    assertEquals(author1.getName(), "SonarQube");
    assertEquals(author1.getSlug(), "sonarqube");
    assertEquals(author1.getEmail(), "sq@email.com");

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getComments().size(), 0);
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(diff3.getComments().size(), 1);
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));

    StashComment comment2 = diff3.getComments().get(0);
    assertEquals(comment2.getId(), 54321);
    assertEquals(comment2.getMessage(), "Test comment 2");
    assertEquals(comment2.getVersion(), 1);

    StashUser author2 = comment2.getAuthor();
    assertEquals(author2.getId(), 54321);
    assertEquals(author2.getName(), "SonarQube2");
    assertEquals(author2.getSlug(), "sonarqube2");
    assertEquals(author2.getEmail(), "sq2@email.com");

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(diff4.getComments().size(), 0);
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));

    StashDiff diff5 = report.getDiffs().get(4);
    assertEquals(diff5.getSource(), 0);
    assertEquals(diff5.getDestination(), 0);
    assertEquals(diff5.getPath(), "stash-plugin/Test.java");
    assertEquals(diff5.getType(), IssueType.CONTEXT);
    assertEquals(diff5.getComments().size(), 2);
    assertFalse(diff5.containsComment(12345));
    assertFalse(diff5.containsComment(54321));
    assertTrue(diff5.containsComment(123456));
    assertTrue(diff5.containsComment(654321));

    StashComment comment3 = diff5.getComments().get(0);
    assertEquals(comment3.getId(), 123456);
    assertEquals(comment3.getMessage(), "Test File comment");
    assertEquals(comment3.getVersion(), 1);

    StashUser author3 = comment3.getAuthor();
    assertEquals(author3.getId(), 12345);
    assertEquals(author3.getName(), "SonarQube");
    assertEquals(author3.getSlug(), "sonarqube");
    assertEquals(author3.getEmail(), "sq@email.com");

    StashComment comment4 = diff5.getComments().get(1);
    assertEquals(comment4.getId(), 654321);
    assertEquals(comment4.getMessage(), "Test File comment 2");
    assertEquals(comment4.getVersion(), 1);

    StashUser author4 = comment4.getAuthor();
    assertEquals(author4.getId(), 54321);
    assertEquals(author4.getName(), "SonarQube2");
    assertEquals(author4.getSlug(), "sonarqube2");
    assertEquals(author4.getEmail(), "sq2@email.com");
  }

  @Test
  public void testExtractDiffsWithEmptyFileComments() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.baseReportWithEmptyFileComments));
    assertEquals(report.getDiffs().size(), 5);

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getComments().size(), 1);
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashComment comment1 = diff1.getComments().get(0);
    assertEquals(comment1.getId(), 12345);
    assertEquals(comment1.getMessage(), "Test comment");
    assertEquals(comment1.getVersion(), 1);

    StashUser author1 = comment1.getAuthor();
    assertEquals(author1.getId(), 12345);
    assertEquals(author1.getName(), "SonarQube");
    assertEquals(author1.getSlug(), "sonarqube");
    assertEquals(author1.getEmail(), "sq@email.com");

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getComments().size(), 0);
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));

    StashDiff diff3 = report.getDiffs().get(2);
    assertEquals(diff3.getComments().size(), 1);
    assertFalse(diff3.containsComment(12345));
    assertTrue(diff3.containsComment(54321));

    StashDiff diff4 = report.getDiffs().get(3);
    assertEquals(diff4.getComments().size(), 0);
    assertFalse(diff4.containsComment(12345));
    assertFalse(diff4.containsComment(54321));

    StashDiff diff5 = report.getDiffs().get(4);
    assertEquals(diff5.getSource(), 0);
    assertEquals(diff5.getDestination(), 0);
    assertEquals(diff5.getPath(), "stash-plugin/Test.java");
    assertEquals(diff5.getType(), IssueType.CONTEXT);
    assertEquals(diff5.getComments().size(), 0);
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
    assertEquals(report.getDiffs().size(), 2);

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long)10);
    assertEquals(diff1.getDestination(), (long)20);
    assertEquals(diff1.getPath(), "stash-plugin/Test.java");
    assertEquals(diff1.getType(), IssueType.CONTEXT);
    assertTrue(diff1.containsComment(12345));
    assertFalse(diff1.containsComment(54321));

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long)20);
    assertEquals(diff2.getDestination(), (long)30);
    assertEquals(diff2.getPath(), "stash-plugin/Test1.java");
    assertEquals(diff2.getType(), IssueType.ADDED);
    assertFalse(diff2.containsComment(12345));
    assertFalse(diff2.containsComment(54321));
  }

  @Test
  public void testExtractDiffsWithDeletedFile() throws Exception {
    StashDiffReport report = StashCollector.extractDiffs(parse(DiffReportSample.deletedFileReport));
    assertEquals(report.getDiffs().size(), 2);

    StashDiff diff1 = report.getDiffs().get(0);
    assertEquals(diff1.getSource(), (long)10);
    assertEquals(diff1.getDestination(), (long)20);
    assertEquals(diff1.getPath(), "stash-plugin/Test2.java");
    assertEquals(diff1.getType(), IssueType.CONTEXT);

    StashDiff diff2 = report.getDiffs().get(1);
    assertEquals(diff2.getSource(), (long)30);
    assertEquals(diff2.getDestination(), (long)40);
    assertEquals(diff2.getPath(), "stash-plugin/Test2.java");
    assertEquals(diff2.getType(), IssueType.ADDED);
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
    assertEquals(pullRequest.getReviewers().size(), 1);
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
    assertEquals(pullRequest.getReviewers().size(), 2);
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

    assertEquals(pullRequest.getReviewers().size(), 0);
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
    assertEquals(user.getId(), userId);
    assertEquals(user.getName(), userName);
    assertEquals(user.getSlug(), userSlug);
    assertEquals(user.getEmail(), userEmail);
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
    assertEquals(true, task.isDeletable());
  }

  private static JsonObject parse(String s) throws Exception {
    return (JsonObject) Jsoner.deserialize(s);
  }
}
