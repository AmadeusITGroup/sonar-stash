package org.sonar.plugins.stash.issue.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;

public class StashCollectorTest {

  private static final long STASH_USER_ID = 1;
 
  @Test
  public void testExtractSimpleComment() throws Exception {
    String commentString = "{\"values\": [{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":0}]}";
    StashCommentReport commentReport = StashCollector.extractComments(commentString);

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
  public void testExtractCommentList() throws Exception {
    String commentString = "{\"values\": ["
        + "{\"id\":1234, \"text\":\"message1\", \"anchor\": {\"path\":\"path1\", \"line\":1},"
          + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":1}, "
        + "{\"id\":5678, \"text\":\"message2\", \"anchor\": {\"path\":\"path2\", \"line\":2},"
          + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\":2}]}";
    
    StashCommentReport commentReport = StashCollector.extractComments(commentString);

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
