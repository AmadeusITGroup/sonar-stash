package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.List;


public class StashComment {

  private final long id;
  private final String message;
  private final StashUser author;
  private final long version;
  private long line;
  private String path;
  private List<StashTask> tasks;

  public StashComment(long id, String message, String path, Long line, StashUser author, long version) {
    this.id = id;
    this.message = message;
    this.path = path;
    this.author = author;
    this.version = version;

    // Stash comment can be null if comment is global to all the file
    if (line == null) {
      this.line = 0;
    } else {
      this.line = line.longValue();
    }

    tasks = new ArrayList<>();
  }

  public long getId() {
    return id;
  }

  public void setLine(long line) {
    this.line = line;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getMessage() {
    return message;
  }

  public String getPath() {
    return path;
  }

  public long getLine() {
    return line;
  }

  public StashUser getAuthor() {
    return author;
  }

  public long getVersion() {
    return version;
  }

  public List<StashTask> getTasks() {
    return tasks;
  }

  public void addTask(StashTask task) {
    tasks.add(task);
  }

  public boolean containsPermanentTasks() {
    boolean result = false;

    for (StashTask task : tasks) {
      if (!task.isDeletable()) {
        result = true;
        break;
      }
    }

    return result;
  }

  @Override
  public boolean equals(Object object) {
    boolean result = false;
    if (object instanceof StashComment) {
      StashComment stashComment = (StashComment)object;
      result = this.getId() == stashComment.getId();
    }

    return result;
  }

  @Override
  public int hashCode() {
    return (int)this.getId();
  }
}
