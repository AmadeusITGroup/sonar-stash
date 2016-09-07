package org.sonar.plugins.stash.client;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class BitbucketCloudV1Api {
    public static final String DEFAULT_PREFIX = "https://api.bitbucket.org/1.0";

    private String prefix;
    private StashCredentials credentials;
    private int timeout;
    private AsyncHttpClient httpClient;

    public BitbucketCloudV1Api(String prefix, StashCredentials credentials, int timeout, AsyncHttpClient httpClient ) {
        this.prefix = prefix;
        this.credentials = credentials;
        this.timeout = timeout;
        this.httpClient = httpClient;
    }

    public BitbucketCloudV1Api(StashCredentials credentials, int timeout , AsyncHttpClient httpClient) {
        this(DEFAULT_PREFIX, credentials, timeout, httpClient);
    }

    public void postCommentOnPullRequest(String project, String repository, String pullRequestId, String report) throws StashClientException {
        Map<String, String> params = new HashMap<>();
        params.put("content", report);
        postCreate(
                url("repositories", project, repository, "pullrequests", pullRequestId, "comments"),
                params
        );
    }

    public StashCommentReport getPullRequestComments(String project, String repository, String pullRequestId, String path) throws StashClientException {
        JSONArray response = getList(
                url("repositories", project, repository, "pullrequests", pullRequestId, "comments")
        );
        StashCommentReport result = new StashCommentReport();

        for (Object o: response) {
            JSONObject commentJson = (JSONObject) o;
            StashComment comment = new StashComment(
                    (Long) commentJson.get("comment_id"),
                    (String) commentJson.get("content"),
                    (String) commentJson.get("filename"),
                    // FIXME handle line_to
                    (Long) commentJson.get("line_from"),
                    // FIXME extract user
                    parseUser((JSONObject) commentJson.get("author_info")),
                    // FIXME create a version interface
                    (Long) commentJson.get("utc_last_updated")
            );
            result.add(comment);
        }

        return result;
    }

    public void deletePullRequestComment(String project, String repository, String pullRequestId, StashComment comment) throws StashClientException {
        delete(url("repositories", project, repository, "pullrequests", pullRequestId, "comments", String.valueOf(comment.getId())));
    }

    public StashUser getUser(String userSlug) throws StashClientException {
        JSONObject response = getObject(url("users", userSlug));
        JSONObject user = (JSONObject) response.get("user");
        return parseUser(user);
    }

    public void close() {
        httpClient.close();
    }

    private StashUser parseUser(JSONObject info) {
        String username = (String) info.get("username");
        return new StashUser(
                // FIXME
                username.hashCode(),
                (String) info.get("first_name") + " " + (String) info.get("last_name"),
                username,
                null
        );
    }

    private void postCreate(String url, Map<String, String> params) {
    }

    private void delete(String url) {
    }

    private JSONArray getList(String url) {
        return null;
    }

    private JSONObject getObject(String url) {
        try {
            Response response = httpClient.prepareGet(url).execute().get();
            return (JSONObject) new JSONParser().parse(response.getResponseBody());
        } catch (InterruptedException | IOException | ExecutionException | ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String url(String... parts) {
        return prefix + '/' + StringUtils.join(parts, '/');
    }
}
