package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.stash.StashPlugin;

/**
 * This class is a representation of the Stash Diff view.
 * 
 * Purpose is to check if a SonarQube issue belongs to the Stash diff view before posting.
 * Indeed, Stash Diff view displays only comments which belong to this view.
 *
 */
public class StashDiffReport {

  private List<StashDiff> diffs;

  public StashDiffReport() {
    this.diffs = new ArrayList();
  }

  public List<StashDiff> getDiffs() {
    return diffs;
  }

  public void add(StashDiff diff) {
    diffs.add(diff);
  }

  public void add(StashDiffReport report) {
    for (StashDiff diff : report.getDiffs()) {
      diffs.add(diff);
    }
  }

  public String getType(String path, long destination) {
    for (StashDiff diff : diffs) {
      // Line 0 never belongs to Stash Diff view.
      // It is a global comment with a type set to CONTEXT.
      if (StringUtils.equals(diff.getPath(), path) && destination == 0) {
        return StashPlugin.CONTEXT_ISSUE_TYPE;
      }
      if (StringUtils.equals(diff.getPath(), path) && diff.getDestination() == destination) {
        return diff.getType();
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
      if (diff.getPath().endsWith(path) && (diff.getDestination() == destination)) {
        if (diff.isTypeOfContext()) {
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

  public String getPath(String path) {
    for (StashDiff diff : diffs) {
      if (diff.getPath().endsWith(path)) {
        return diff.getPath();
      }
    }
    return null;
  }

}
