package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class StashPullRequest {

  private final String project;
  private final String repository;
  private final String id;

  private long version;

  private final ArrayList<StashUser> reviewers;

  public StashPullRequest(String project, String repository, String pullRequestId) {
    this.project = project;
    this.repository = repository;
    this.id = pullRequestId;

    this.reviewers = new ArrayList<>();
  }

  public String getProject() {
    return project;
  }

  public String getRepository() {
    return repository;
  }

  public String getId() {
    return id;
  }

  public long getVersion() {
    return version;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void addReviewer(StashUser reviewer) {
    this.reviewers.add(reviewer);
  }

  public List<StashUser> getReviewers() {
    return reviewers;
  }
  
  public StashUser getReviewer(String user) {
    StashUser result = null;
    for (StashUser stashReviewer : reviewers) {
      if (StringUtils.equals(user, stashReviewer.getSlug())) {
        result = stashReviewer;
        break;
      }
    }

    return result;
  }

  public boolean containsReviewer(StashUser reviewer) {
    boolean result = false;
    for (StashUser stashReviewer : reviewers) {
      if (reviewer.getId() == stashReviewer.getId()) {
        result = true;
        break;
      }
    }

    return result;
  }
}
