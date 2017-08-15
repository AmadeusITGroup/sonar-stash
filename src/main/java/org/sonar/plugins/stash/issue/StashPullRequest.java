package org.sonar.plugins.stash.issue;

import org.sonar.plugins.stash.PullRequestRef;

import java.util.ArrayList;
import java.util.List;

public class StashPullRequest {

  private final PullRequestRef ref;

  private long version;

  private final ArrayList<StashUser> reviewers;

  public int getId() {
    return ref.pullRequestId();
  }

  public String getProject() {
    return ref.project();
  }

  public String getRepository() {
    return ref.repository();
  }

  public StashPullRequest(PullRequestRef pr) {
    this.ref = pr;

    this.reviewers = new ArrayList<>();
  }

  public PullRequestRef getRef() {
    return ref;
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
      if (user.equals(stashReviewer.getSlug())) {
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
