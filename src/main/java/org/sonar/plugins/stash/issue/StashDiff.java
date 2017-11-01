package org.sonar.plugins.stash.issue;

import org.sonar.plugins.stash.StashPlugin;

import java.util.ArrayList;
import java.util.List;
import org.sonar.plugins.stash.StashPlugin.IssueType;

public class StashDiff {

  private final IssueType type;
  private final String path;
  private final long source;
  private final long destination;
  private final List<StashComment> comments;

  public StashDiff(IssueType type, String path, long source, long destination) {
    this.type = type;
    this.path = path;
    this.source = source;
    this.destination = destination;
    this.comments = new ArrayList<>();
  }

  public void addComment(StashComment comment) {
    this.comments.add(comment);
  }

  public String getPath() {
    return path;
  }

  public long getSource() {
    return source;
  }

  public long getDestination() {
    return destination;
  }

  public IssueType getType() {
    return type;
  }

  public List<StashComment> getComments() {
    return comments;
  }

  public boolean containsComment(long commentId) {
    return comments.stream().anyMatch(c -> c.getId() == commentId);
  }
}
