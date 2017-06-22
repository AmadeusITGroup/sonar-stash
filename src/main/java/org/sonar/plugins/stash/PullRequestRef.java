package org.sonar.plugins.stash;

public final class PullRequestRef {
  private String project;
  private String repository;
  private int pullRequestId;

  private PullRequestRef(String project, String repository, int pullRequestId) {
    this.project = project;
    this.repository = repository;
    this.pullRequestId = pullRequestId;
  }

  public String project() {
    return project;
  }

  public String repository() {
    return repository;
  }

  public int pullRequestId() {
    return pullRequestId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String project;
    private String repository;
    private int pullRequestId;

    public Builder setProject(String value) {
      project = value;
      return this;
    }

    public Builder setRepository(String value) {
      repository = value;
      return this;
    }

    public Builder setPullRequestId(int value) {
      pullRequestId = value;
      return this;
    }

    public PullRequestRef build() {
      return new PullRequestRef(project, repository, pullRequestId);
    }
  }
}
