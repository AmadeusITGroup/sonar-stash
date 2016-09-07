package org.sonar.plugins.stash.client;

import com.ning.http.client.AsyncHttpClient;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

import java.util.ArrayList;

public class BitbucketCloudClient implements StashApi {
    private BitbucketCloudV1Api v1Api;
    private BitbucketCloudV2Api v2Api;

    public BitbucketCloudClient(StashCredentials credentials, int timeout) {
        AsyncHttpClient httpClient = new AsyncHttpClient();
        this.v1Api = new BitbucketCloudV1Api(credentials, timeout, httpClient);
        this.v2Api = new BitbucketCloudV2Api(credentials, timeout, httpClient);
    }

    @Override
    public void postCommentOnPullRequest(String project, String repository, String pullRequestId, String report) throws StashClientException {
        v1Api.postCommentOnPullRequest(project, repository, pullRequestId, report);
    }

    @Override
    public StashCommentReport getPullRequestComments(String project, String repository, String pullRequestId, String path) throws StashClientException {
        return v1Api.getPullRequestComments(project, repository, pullRequestId, path);
    }

    @Override
    public void deletePullRequestComment(String project, String repository, String pullRequestId, StashComment comment) throws StashClientException {
        v1Api.deletePullRequestComment(project, repository, pullRequestId, comment);
    }

    @Override
    public StashDiffReport getPullRequestDiffs(String project, String repository, String pullRequestId) throws StashClientException {
        return v2Api.getPullRequestDiffs(project, repository, pullRequestId);
    }

    @Override
    public StashComment postCommentLineOnPullRequest(String project, String repository, String pullRequestId, String message, String path, long line, String type) throws StashClientException {
        return null;
    }

    @Override
    public StashUser getUser(String userSlug) throws StashClientException {
        return null;
    }

    @Override
    public StashPullRequest getPullRequest(String project, String repository, String pullRequestId) throws StashClientException {
        return null;
    }

    @Override
    public void addPullRequestReviewer(String project, String repository, String pullRequestId, long pullRequestVersion, ArrayList<StashUser> reviewers) throws StashClientException {

    }

    @Override
    public void approvePullRequest(String project, String repository, String pullRequestId) throws StashClientException {

    }

    @Override
    public void resetPullRequestApproval(String project, String repository, String pullRequestId) throws StashClientException {

    }

    @Override
    public void postTaskOnComment(String message, Long commentId) throws StashClientException {

    }

    @Override
    public void deleteTaskOnComment(StashTask task) throws StashClientException {

    }

    @Override
    public void close() {
        v1Api.close();
        v2Api.close();
    }
}
