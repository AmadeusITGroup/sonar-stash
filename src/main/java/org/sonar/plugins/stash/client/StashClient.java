package org.sonar.plugins.stash.client;

import java.util.ArrayList;

import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

public interface StashClient extends AutoCloseable {

  String getBaseUrl();

  void postCommentOnPullRequest(PullRequestRef pr, String printReportMarkdown) throws StashClientException;

  void approvePullRequest(PullRequestRef pr) throws StashClientException;

  Object getLogin();

  void resetPullRequestApproval(PullRequestRef pr) throws StashClientException;

  StashPullRequest getPullRequest(PullRequestRef pr) throws StashClientException;

  StashUser getUser(String user) throws StashClientException;

  void addPullRequestReviewer(PullRequestRef pr, long version, ArrayList<StashUser> reviewers) throws StashClientException;

  StashCommentReport getPullRequestComments(PullRequestRef pr, String path) throws StashClientException;

  StashComment postCommentLineOnPullRequest(PullRequestRef pr, String printIssueMarkdown,
      String path, long line, String type) throws StashClientException;

  void postTaskOnComment(String message, Long commentId) throws StashClientException;

  StashDiffReport getPullRequestDiffs(PullRequestRef pr) throws StashClientException;

  void deleteTaskOnComment(StashTask task) throws StashClientException;

  void deletePullRequestComment(PullRequestRef pr, StashComment comment) throws StashClientException;

}
