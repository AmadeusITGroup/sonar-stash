package org.sonar.plugins.stash.issue;

public class StashTask {

  private final Long id;
  private final String text;
  private final String state;
  private final boolean deletable;

  public StashTask(Long id, String text, String state, boolean deletable) {
    this.id = id;
    this.text = text;
    this.state = state;
    this.deletable = deletable;
  }

  public Long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public String getState() {
    return state;
  }

  public boolean isDeletable() {
    return deletable;
  }
}
