package org.sonar.plugins.stash.client;

import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

import java.util.ArrayList;

public interface StashApi extends AutoCloseable {
    void postCommentOnPullRequest(String project, String repository, String pullRequestId, String report)
        throws StashClientException;

    StashCommentReport getPullRequestComments(String project, String repository, String pullRequestId, String path)
            throws StashClientException;

    void deletePullRequestComment(String project, String repository, String pullRequestId, StashComment comment)
                throws StashClientException;

    StashDiffReport getPullRequestDiffs(String project, String repository, String pullRequestId)
                    throws StashClientException;

    StashComment postCommentLineOnPullRequest(String project, String repository, String pullRequestId, String message, String path, long line, String type)
                        throws StashClientException;

    StashUser getUser(String userSlug)
                                throws StashClientException;

    StashPullRequest getPullRequest(String project, String repository, String pullRequestId)
                                    throws StashClientException;

    void addPullRequestReviewer(String project, String repository, String pullRequestId, long pullRequestVersion, ArrayList<StashUser> reviewers)
                                        throws StashClientException;

    void approvePullRequest(String project, String repository, String pullRequestId) throws StashClientException;

    void resetPullRequestApproval(String project, String repository, String pullRequestId) throws StashClientException;

    void postTaskOnComment(String message, Long commentId) throws StashClientException;

    void deleteTaskOnComment(StashTask task) throws StashClientException;

    @Override
    void close();
}
