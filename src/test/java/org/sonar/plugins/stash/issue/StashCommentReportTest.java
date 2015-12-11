package org.sonar.plugins.stash.issue;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StashCommentReportTest {

  @Test
  public void testContains() {
    long id = 1;
    String message1 = "message1";
    String path = "path";
    long line = 1;

    StashComment comment1 = new StashComment(id, message1, path, line);
    StashCommentReport report = new StashCommentReport();
    report.add(comment1);

    assertTrue(report.contains(message1, path, line));

    String message2 = "message2";

    StashComment comment2 = new StashComment(id, message2, path, line);
    report = new StashCommentReport();
    report.add(comment2);

    assertTrue(report.contains(message2, path, line));
    assertFalse(report.contains("message3", path, line));
  }

  @Test
  public void testNotContains() {
    long id = 1;
    String message = "msg1";
    String path = "path";
    long line = 1;

    StashComment comment = new StashComment(id, message, path, line);
    StashCommentReport report = new StashCommentReport();
    report.add(comment);

    assertFalse(report.contains("message", path, line));
    assertFalse(report.contains(message, "p", line));
    assertFalse(report.contains(message, path, (long) 2));
  }

  @Test
  public void testSize() {
    StashCommentReport report = new StashCommentReport();

    StashComment comment1 = new StashComment(1, "message1", "path1", (long) 1);
    report.add(comment1);
    assertEquals(report.size(), 1);

    StashComment comment2 = new StashComment(2, "message2", "path2", (long) 1);
    report.add(comment2);
    assertEquals(report.size(), 2);
  }

  @Test
  public void testSizeOfEmptyReport() {
    StashCommentReport report = new StashCommentReport();
    assertEquals(report.size(), 0);
  }

  @Test
  public void testAddReport() {
    StashCommentReport report1 = new StashCommentReport();

    StashComment comment1 = new StashComment(1, "message1", "path", (long) 1);
    StashComment comment2 = new StashComment(2, "message2", "path", (long) 1);
    report1.add(comment1);
    report1.add(comment2);

    StashCommentReport report2 = new StashCommentReport();
    StashComment comment3 = new StashComment(3, "message3", "path", (long) 1);
    report2.add(comment3);

    report2.add(report1);
    assertEquals(report2.size(), 3);

    assertTrue(report2.contains("message1", "path", (long) 1));
    assertTrue(report2.contains("message2", "path", (long) 1));
    assertTrue(report2.contains("message3", "path", (long) 1));
    assertFalse(report2.contains("message4", "path", (long) 1));
  }

  @Test
  public void testAddEmptyReportToNotEmptyReport() {
    StashCommentReport report1 = new StashCommentReport();
    StashComment comment1 = new StashComment(1, "message1", "path", (long) 1);
    StashComment comment2 = new StashComment(2, "message2", "path", (long) 2);
    report1.add(comment1);
    report1.add(comment2);

    StashCommentReport report2 = new StashCommentReport();
    report2.add(report1);
    assertEquals(report2.size(), 2);
  }

  public void testAddNotEmptyReportToEmptyReport() {
    StashCommentReport report1 = new StashCommentReport();

    StashCommentReport report2 = new StashCommentReport();
    StashComment comment = new StashComment(1, "message", "path", (long) 1);
    report2.add(comment);

    report1.add(report2);
    assertEquals(report1.size(), 1);
  }

  @Test
  public void testAddEmptyReportToEmptyReport() {
    StashCommentReport report1 = new StashCommentReport();
    StashCommentReport report2 = new StashCommentReport();

    report1.add(report2);
    assertEquals(report1.size(), 0);
  }

  @Test
  public void applyDiffReportWithCONTEXT() {
    StashDiff diff = mock(StashDiff.class);
    when(diff.isTypeOfContext()).thenReturn(true);
    when(diff.getDestination()).thenReturn((long) 10);

    StashDiffReport diffReport = mock(StashDiffReport.class);
    when(diffReport.getDiffByComment(987654)).thenReturn(diff);

    StashComment comment1 = new StashComment(123456, "message1", "path1", (long) 1);
    StashComment comment2 = new StashComment(987654, "message2", "path2", (long) 2);

    StashCommentReport report = new StashCommentReport();
    report.add(comment1);
    report.add(comment2);

    report.applyDiffReport(diffReport);
    assertTrue(report.contains("message1", "path1", 1));
    assertTrue(report.contains("message2", "path2", 10));
  }

  @Test
  public void applyDiffReportWithADDED() {
    StashDiff diff = mock(StashDiff.class);
    when(diff.isTypeOfContext()).thenReturn(false);
    when(diff.getDestination()).thenReturn((long) 10);

    StashDiffReport diffReport = mock(StashDiffReport.class);
    when(diffReport.getDiffByComment(987654)).thenReturn(diff);

    StashComment comment1 = new StashComment(123456, "message1", "path1", (long) 1);
    StashComment comment2 = new StashComment(987654, "message2", "path2", (long) 2);

    StashCommentReport report = new StashCommentReport();
    report.add(comment1);
    report.add(comment2);

    report.applyDiffReport(diffReport);
    assertTrue(report.contains("message1", "path1", 1));
    assertTrue(report.contains("message2", "path2", 2));
    assertFalse(report.contains("message2", "path2", 10));
  }
}
