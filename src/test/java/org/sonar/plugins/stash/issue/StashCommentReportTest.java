package org.sonar.plugins.stash.issue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.plugins.stash.StashPlugin.IssueType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StashCommentReportTest {

  @Mock
  StashComment comment1;

  @Mock
  StashComment comment2;

  @Before
  public void setUp() {
    comment1 = mock(StashComment.class);
    when(comment1.getId()).thenReturn((long)123456);
    when(comment1.getLine()).thenReturn((long)1);
    when(comment1.getMessage()).thenReturn("message1");
    when(comment1.getPath()).thenReturn("path1");

    comment2 = mock(StashComment.class);
    when(comment2.getId()).thenReturn((long)987654);
    when(comment2.getLine()).thenReturn((long)2);
    when(comment2.getMessage()).thenReturn("message2");
    when(comment2.getPath()).thenReturn("path2");
  }

  @Test
  public void testContains() {
    StashCommentReport report = new StashCommentReport();
    report.add(comment1);

    assertTrue(report.contains("message1", "path1", 1));

    report = new StashCommentReport();
    report.add(comment2);

    assertTrue(report.contains("message2", "path2", 2));
    assertFalse(report.contains("message3", "path2", 2));
  }

  @Test
  public void testNotContains() {
    StashCommentReport report = new StashCommentReport();
    report.add(comment1);

    assertFalse(report.contains("message", "path1", 1));
    assertFalse(report.contains("message1", "path", 1));
    assertFalse(report.contains("message1", "path1", (long)2));
  }

  @Test
  public void testSize() {
    StashCommentReport report = new StashCommentReport();

    report.add(comment1);
    assertEquals(report.size(), 1);

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
    report1.add(comment1);
    report1.add(comment2);

    StashComment comment3 = mock(StashComment.class);
    when(comment3.getLine()).thenReturn((long)3);
    when(comment3.getMessage()).thenReturn("message3");
    when(comment3.getPath()).thenReturn("path3");

    StashCommentReport report2 = new StashCommentReport();
    report2.add(comment3);

    report2.add(report1);
    assertEquals(report2.size(), 3);

    assertTrue(report2.contains("message1", "path1", (long)1));
    assertTrue(report2.contains("message2", "path2", (long)2));
    assertTrue(report2.contains("message3", "path3", (long)3));
    assertFalse(report2.contains("message4", "path4", (long)4));
  }

  @Test
  public void testAddEmptyReportToNotEmptyReport() {
    StashCommentReport report1 = new StashCommentReport();
    report1.add(comment1);
    report1.add(comment2);

    StashCommentReport report2 = new StashCommentReport();
    report2.add(report1);
    assertEquals(report2.size(), 2);
  }

  @Test
  public void testAddNotEmptyReportToEmptyReport() {
    StashCommentReport report1 = new StashCommentReport();

    StashCommentReport report2 = new StashCommentReport();
    report2.add(comment1);

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
    when(diff.getType()).thenReturn(IssueType.CONTEXT);
    when(diff.getDestination()).thenReturn((long)10);

    StashDiffReport diffReport = mock(StashDiffReport.class);
    when(diffReport.getDiffByComment(987654)).thenReturn(diff);

    StashUser stashUser = mock(StashUser.class);
    comment2 = new StashComment(987654, "message2", "path2", (long)2, stashUser, (long)0);

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
    when(diff.getType()).thenReturn(IssueType.ADDED);
    when(diff.getDestination()).thenReturn((long)10);

    StashDiffReport diffReport = mock(StashDiffReport.class);
    when(diffReport.getDiffByComment(987654)).thenReturn(diff);

    StashCommentReport report = new StashCommentReport();
    report.add(comment1);
    report.add(comment2);

    report.applyDiffReport(diffReport);
    assertTrue(report.contains("message1", "path1", 1));
    assertTrue(report.contains("message2", "path2", 2));
    assertFalse(report.contains("message2", "path2", 10));
  }
}
