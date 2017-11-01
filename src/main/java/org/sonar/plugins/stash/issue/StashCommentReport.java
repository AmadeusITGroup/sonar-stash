package org.sonar.plugins.stash.issue;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.stash.StashPlugin.IssueType;

public class StashCommentReport {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashCommentReport.class);

  private List<StashComment> comments;

  public StashCommentReport() {
    this.comments = new ArrayList<>();
  }

  public List<StashComment> getComments() {
    return comments;
  }

  public void add(StashComment comment) {
    comments.add(comment);
  }

  public void add(StashCommentReport report) {
    for (StashComment comment : report.getComments()) {
      comments.add(comment);
    }
  }

  public boolean contains(String message, String path, long line) {
    boolean result = false;
    for (StashComment comment : comments) {

      if (Objects.equals(comment.getMessage(), message)
       && Objects.equals(comment.getPath(), path)
       && comment.getLine() == line) {

        result = true;
        break;
      }
    }

    return result;
  }

  public StashCommentReport applyDiffReport(StashDiffReport diffReport) {
    for (StashComment comment : comments) {
      StashDiff diff = diffReport.getDiffByComment(comment.getId());
      if ((diff != null) && diff.getType() == IssueType.CONTEXT) {

        // By default comment line, with type == CONTEXT, is set to FROM value.
        // Set comment line to TO value to be compared with SonarQube issue.
        long destination = diff.getDestination();
        comment.setLine(destination);

        LOGGER.debug("Update Stash comment \"{}\": set comment line to destination diff line ({})",
                     comment.getId(),
                     comment.getLine());
      }
    }

    return this;
  }

  public int size() {
    return comments.size();
  }

}
