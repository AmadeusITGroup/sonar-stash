package org.sonar.plugins.stash.issue;

import com.google.common.collect.Range;
import java.util.Objects;
import org.sonar.plugins.stash.StashPlugin;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.stash.StashPlugin.IssueType;

/**
 * This class is a representation of the Stash Diff view.
 * <p>
 * Purpose is to check if a SonarQube issue belongs to the Stash diff view before posting.
 * Indeed, Stash Diff view displays only comments which belong to this view.
 */
public class StashDiffReport {

  public static final int VICINITY_RANGE_NONE = 0;

  private List<StashDiff> diffs;

  public StashDiffReport() {
    this.diffs = new ArrayList<>();
  }

  public List<StashDiff> getDiffs() {
    return diffs;
  }

  public void add(StashDiff diff) {
    diffs.add(diff);
  }

  public void add(StashDiffReport report) {
    diffs.addAll(report.getDiffs());
  }

  private static boolean includeVicinityIssuesForDiff(StashDiff diff, long destination, int range) {
    if (range <= 0) {
      return false;
    }
    return Range.closed(diff.getSource() - range, diff.getDestination() + range).contains(destination);
  }

  public IssueType getType(String path, long destination, int vicinityRange) {
    for (StashDiff diff : diffs) {
      if (Objects.equals(diff.getPath(), path)) {
        // Line 0 never belongs to Stash Diff view.
        // It is a global comment with a type set to CONTEXT.
        if (destination == 0) {
          return IssueType.CONTEXT;
        } else if (destination == diff.getDestination() || includeVicinityIssuesForDiff(diff,
            destination, vicinityRange)) {
          return diff.getType();
        }
      }
    }
    return null;
  }

  /**
   * Depends on the type of the diff.
   * If type == "CONTEXT", return the source line of the diff.
   * If type == "ADDED", return the destination line of the diff.
   */
  public long getLine(String path, long destination) {
    for (StashDiff diff : diffs) {
      if (Objects.equals(diff.getPath(), path) && (diff.getDestination() == destination)) {

        if (diff.getType() == IssueType.CONTEXT) {
          return diff.getSource();
        } else {
          return diff.getDestination();
        }
      }
    }
    return 0;
  }

  public StashDiff getDiffByComment(long commentId) {
    for (StashDiff diff : diffs) {
      if (diff.containsComment(commentId)) {
        return diff;
      }
    }
    return null;
  }

  /**
   * Get all comments from the Stash differential report.
   */
  public List<StashComment> getComments() {
    List<StashComment> result = new ArrayList<>();

    for (StashDiff diff : this.diffs) {
      List<StashComment> comments = diff.getComments();

      for (StashComment comment : comments) {
        if (!result.contains(comment)) {
          result.add(comment);
        }
      }
    }
    return result;
  }
}
