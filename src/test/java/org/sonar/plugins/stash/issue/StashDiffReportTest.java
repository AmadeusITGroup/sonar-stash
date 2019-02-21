package org.sonar.plugins.stash.issue;

import com.google.common.collect.Range;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.stash.StashPlugin.IssueType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StashDiffReportTest {

  StashDiff diff1;
  StashDiff diff2;
  StashDiff diff3;
  StashDiff diff4;
  StashDiff diff5;
  StashDiff diff6;

  StashDiffReport report1 = new StashDiffReport();
  StashDiffReport report2 = new StashDiffReport();

  private static final String FILE_PATH = "path/to/diff";

  @BeforeEach
  public void setUp() {
    StashComment comment1 = mock(StashComment.class);
    when(comment1.getId()).thenReturn((long)12345);

    StashComment comment2 = mock(StashComment.class);
    when(comment2.getId()).thenReturn((long)54321);

    diff1 = new StashDiff(IssueType.CONTEXT, "path/to/diff1", (long)10, (long)20);
    diff1.addComment(comment1);

    diff2 = new StashDiff(IssueType.ADDED, "path/to/diff2", (long)20, (long)30);
    diff2.addComment(comment2);

    diff3 = new StashDiff(IssueType.CONTEXT, "path/to/diff3", (long)30, (long)40);

    report1.add(diff1);
    report1.add(diff2);
    report1.add(diff3);

    diff4 = new StashDiff(IssueType.CONTEXT, FILE_PATH, (long)10, (long)10);
    diff5 = new StashDiff(IssueType.ADDED, FILE_PATH, (long)11, (long)11);
    diff6 = new StashDiff(IssueType.CONTEXT, FILE_PATH, (long)11, (long)12);
    report2.add(diff4);
    report2.add(diff5);
    report2.add(diff6);
  }

  @Test
  public void testAdd() {
    StashDiffReport report = new StashDiffReport();
    assertEquals(0, report.getDiffs().size());

    report.add(diff1);
    assertEquals(1, report.getDiffs().size());

    StashDiff result1 = report.getDiffs().get(0);
    assertEquals("path/to/diff1", result1.getPath());
    assertEquals(IssueType.CONTEXT, result1.getType());
    assertEquals(10, result1.getSource());
    assertEquals(20, result1.getDestination());

    report.add(diff2);
    assertEquals(2, report.getDiffs().size());

    StashDiff result2 = report.getDiffs().get(1);
    assertEquals("path/to/diff2", result2.getPath());
    assertEquals(IssueType.ADDED, result2.getType());
    assertEquals(20, result2.getSource());
    assertEquals(30, result2.getDestination());
  }

  @Test
  public void testAddReport() {
    assertEquals(3, report1.getDiffs().size());

    StashDiffReport report = new StashDiffReport();
    assertEquals(0, report.getDiffs().size());

    report.add(report1);
    assertEquals(3, report.getDiffs().size());
  }

  @Test
  public void testGetType() {
    assertNull(report1.getType("path/to/diff1", 20, StashDiffReport.VICINITY_RANGE_NONE));
    assertEquals(IssueType.ADDED, report1.getType("path/to/diff2", 30, StashDiffReport.VICINITY_RANGE_NONE));

    assertNull(report1.getType("path/to/diff2", 20, StashDiffReport.VICINITY_RANGE_NONE));
    assertNull(report1.getType("path/to/diff1", 30, StashDiffReport.VICINITY_RANGE_NONE));
    assertNull(report1.getType("path/to/diff4", 60, StashDiffReport.VICINITY_RANGE_NONE));
  }

  @Test
  public void testGetTypeWithNoDestination() {
    assertEquals(IssueType.CONTEXT, report1.getType("path/to/diff1", 0, StashDiffReport.VICINITY_RANGE_NONE));
    assertNull(report1.getType("path/to/diff", 0, StashDiffReport.VICINITY_RANGE_NONE));
  }

  @Test
  public void testGetTypeVicinity() {
    assertEquals(IssueType.CONTEXT, report2.getType(FILE_PATH, 10, 1));
    assertNull(report2.getType(FILE_PATH, 10, StashDiffReport.VICINITY_RANGE_NONE));
    assertEquals(IssueType.ADDED, report2.getType(FILE_PATH, 11, 1));
    assertEquals(IssueType.CONTEXT, report2.getType(FILE_PATH, 12, 1));
    assertNull(report2.getType(FILE_PATH, 12, StashDiffReport.VICINITY_RANGE_NONE));
  }

  @Test
  public void testGetLine() {
    assertEquals(10, report1.getLine("path/to/diff1", 20));
    assertEquals(30, report1.getLine("path/to/diff2", 30));
    assertEquals(30, report1.getLine("path/to/diff3", 40));

    assertEquals(0, report1.getLine("path/to/diff1", 50));
  }

  @Test
  public void testGetDiffByComment() {
    StashDiff diff1 = report1.getDiffByComment(12345);
    assertEquals("path/to/diff1", diff1.getPath());
    assertEquals(IssueType.CONTEXT, diff1.getType());
    assertEquals(10, diff1.getSource());
    assertEquals(20, diff1.getDestination());

    StashDiff diff2 = report1.getDiffByComment(123456);
    assertNull(diff2);
  }

  @Test
  public void testGetComments() {
    List<StashComment> comments = report1.getComments();

    assertEquals(2, comments.size());
    assertEquals(12345, comments.get(0).getId());
    assertEquals(54321, comments.get(1).getId());
  }

  @Test
  public void testGetCommentsWithoutAnyIssues() {
    StashDiffReport report = new StashDiffReport();
    List<StashComment> comments = report.getComments();
    assertEquals(0, comments.size());
  }

  @Test
  public void testGetCommentsWithDuplicatedComments() {
    StashComment comment1 = new StashComment((long)12345, "message", "path", (long)1, mock(StashUser.class), (long)1);
    diff1.addComment(comment1);

    StashComment comment2 = new StashComment((long)12345, "message", "path", (long)1, mock(StashUser.class), (long)1);
    diff2.addComment(comment2);

    StashComment comment3 = new StashComment((long)54321, "message", "path", (long)1, mock(StashUser.class), (long)1);
    diff3.addComment(comment3);

    List<StashComment> comments = report1.getComments();

    assertEquals(2, comments.size());
    assertEquals(12345, comments.get(0).getId());
    assertEquals(54321, comments.get(1).getId());
  }

  @Test
  public void testVicinitySourceBeforeDestination() {
    // https://github.com/AmadeusITGroup/sonar-stash/issues/189
    final String path = "some/path";
    StashDiffReport report = new StashDiffReport();
    report.add(
        new StashDiff(IssueType.REMOVED, path, 165, 132)
    );
    assertEquals(
        IssueType.CONTEXT,
        report.getType("some/path", 164, 2)
    );
  }
}
