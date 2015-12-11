package org.sonar.plugins.stash.issue.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.builder.DiffResult;
import org.junit.Test;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiff;
import org.sonar.plugins.stash.issue.StashDiffReport;

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
}
