package org.sonar.plugins.stash.client;

import org.apache.commons.lang3.StringUtils;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;
import org.asynchttpclient.config.AsyncHttpClientConfigDefaults;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sonar.plugins.stash.PluginInfo;
import org.sonar.plugins.stash.PluginUtils;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashReportExtractionException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;
import org.sonar.plugins.stash.issue.collector.StashCollector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StashClient implements AutoCloseable {

  private final String baseUrl;
  private final StashCredentials credentials;
  private final int stashTimeout;
  private AsyncHttpClient httpClient;

  private static final String REST_API = "/rest/api/1.0/";
  private static final String USER_API = "{0}" + REST_API + "users/{1}";
  private static final String REPO_API = "{0}" + REST_API + "projects/{1}/repos/{2}/";
  private static final String TASKS_API = REST_API + "tasks";

  private static final String API_PULLREQUESTS = REPO_API + "pull-requests";
  private static final String API_PULLREQUEST_BY_BRANCH = API_PULLREQUESTS + "?direction=outgoing&at=refs/heads/{3}";
  private static final String API_ONE_PR = API_PULLREQUESTS + "/{3,number,#}";

  private static final String API_ONE_PR_ALL_COMMENTS = API_ONE_PR + "/comments";
  private static final String API_ONE_PR_DIFF = API_ONE_PR + "/diff?withComments=true";
  private static final String API_ONE_PR_APPROVAL = API_ONE_PR + "/approve";
  private static final String API_ONE_PR_COMMENT_PATH = API_ONE_PR + "/comments?path={4}&start={5,number,#}";

  private static final String API_ONE_PR_ONE_COMMENT = API_ONE_PR_ALL_COMMENTS + "/{4}?version={5}";

  private static final String PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE = "Unable to change status of pull-request {0} #{1,number,#}.";
  private static final String PULL_REQUEST_GET_ERROR_MESSAGE = "Unable to retrieve pull-request {0} #{1,number,#}.";
  private static final String PULL_REQUEST_PUT_ERROR_MESSAGE = "Unable to update pull-request {0} #{1,number,#}.";
  private static final String USER_GET_ERROR_MESSAGE = "Unable to retrieve user {0}.";
  private static final String COMMENT_POST_ERROR_MESSAGE = "Unable to post a comment to {0} #{1,number,#}.";
  private static final String COMMENT_GET_ERROR_MESSAGE = "Unable to get comment linked to {0} #{1,number,#}.";
  private static final String COMMENT_DELETION_ERROR_MESSAGE = "Unable to delete comment {0,number,#} from pull-request {1} #{2,number,#}.";
  private static final String TASK_POST_ERROR_MESSAGE = "Unable to post a task on comment {0,number,#}.";
  private static final String TASK_DELETION_ERROR_MESSAGE = "Unable to delete task {0,number,#}.";

  private static final ContentType JSON = new ContentType("application", "json", null);

  public StashClient(String url, StashCredentials credentials, int stashTimeout, String sonarQubeVersion) {
    this.baseUrl = url;
    this.credentials = credentials;
    this.stashTimeout = stashTimeout;
    this.httpClient = createHttpClient(sonarQubeVersion);
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getLogin() {
    return credentials.getLogin();
  }

  public void postCommentOnPullRequest(PullRequestRef pr, String report)
      throws StashClientException {

    String request = MessageFormat.format(API_ONE_PR_ALL_COMMENTS, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
    JSONObject json = new JSONObject();
    json.put("text", report);

    postCreate(request, json, MessageFormat.format(COMMENT_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }

  public StashCommentReport getPullRequestComments(PullRequestRef pr, String path)
      throws StashClientException {
    StashCommentReport result = new StashCommentReport();

    long start = 0;
    boolean isLastPage = false; 
    
    while (! isLastPage){
      try {
        String request = MessageFormat.format(API_ONE_PR_COMMENT_PATH, baseUrl, pr.project(), pr.repository(), pr.pullRequestId(), path, start);
        JSONObject jsonComments = get(request, MessageFormat.format(COMMENT_GET_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
        result.add(StashCollector.extractComments(jsonComments));

        // Stash pagination: check if you get all comments linked to the pull-request
        isLastPage = StashCollector.isLastPage(jsonComments);
        start = StashCollector.getNextPageStart(jsonComments);
      } catch (StashReportExtractionException e) {
        throw new StashClientException(e);
      }
    }
  
    return result;
  } 
  
  public void deletePullRequestComment(PullRequestRef pr, StashComment comment)
      throws StashClientException {

    String request = MessageFormat.format(API_ONE_PR_ONE_COMMENT, baseUrl, pr.project(), pr.repository(), pr.pullRequestId(),
                                          Long.toString(comment.getId()), Long.toString(comment.getVersion()));

    delete(request, MessageFormat.format(COMMENT_DELETION_ERROR_MESSAGE, comment.getId(), pr.repository(), pr.pullRequestId()));
  }

  public StashDiffReport getPullRequestDiffs(PullRequestRef pr)
      throws StashClientException {
    StashDiffReport result = null;

    try {
      String request = MessageFormat.format(API_ONE_PR_DIFF, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
      JSONObject jsonDiffs = get(request, MessageFormat.format(COMMENT_GET_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
      result = StashCollector.extractDiffs(jsonDiffs);
    } catch (StashReportExtractionException e) {
      throw new StashClientException(e);
    }

    return result;
  }

  public StashComment postCommentLineOnPullRequest(PullRequestRef pr, String message, String path, long line, String type)
      throws StashClientException {
    String request = MessageFormat.format(API_ONE_PR_ALL_COMMENTS, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());

    JSONObject anchor = new JSONObject();
    if (line != 0L) {
      anchor.put("line", line);
      anchor.put("lineType", type);
    }
    
    String fileType = "TO";
    if (StringUtils.equals(type, StashPlugin.CONTEXT_ISSUE_TYPE)){
      fileType = "FROM";
    }
    anchor.put("fileType", fileType);
    
    anchor.put("path", path);
    
    JSONObject json = new JSONObject();
    json.put("text", message);
    json.put("anchor", anchor);

    JSONObject response = postCreate(request, json,
    		                         MessageFormat.format(COMMENT_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
    
    return StashCollector.extractComment(response, path, line);
  }

  public StashUser getUser(String userSlug) throws StashClientException {

    String request = MessageFormat.format(USER_API, baseUrl, userSlug);
    JSONObject response = get(request, MessageFormat.format(USER_GET_ERROR_MESSAGE, userSlug));

    return StashCollector.extractUser(response);
  }
  
  public StashPullRequest getPullRequest(PullRequestRef pr)
      throws StashClientException {
    String request = MessageFormat.format(API_ONE_PR, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
    JSONObject response = get(request, MessageFormat.format(PULL_REQUEST_GET_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));

    return StashCollector.extractPullRequest(pr, response);
  }
  
  public void addPullRequestReviewer(PullRequestRef pr, long pullRequestVersion, ArrayList<StashUser> reviewers)
      throws StashClientException {
    String request = MessageFormat.format(API_ONE_PR, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());

    JSONObject json = new JSONObject();

    JSONArray jsonReviewers = new JSONArray();
    for (StashUser reviewer: reviewers) {
      JSONObject reviewerName = new JSONObject();
      reviewerName.put("name", reviewer.getName());

      JSONObject user = new JSONObject();
      user.put("user", reviewerName);

      jsonReviewers.add(user);
    }
    
    json.put("reviewers", jsonReviewers);
    json.put("id", pr.pullRequestId());
    json.put("version", pullRequestVersion);

    put(request, json, MessageFormat.format(PULL_REQUEST_PUT_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }

  public void approvePullRequest(PullRequestRef pr) throws StashClientException {
    String request = MessageFormat.format(API_ONE_PR_APPROVAL, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
    post(request, null, MessageFormat.format(PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }
  
  public void resetPullRequestApproval(PullRequestRef pr) throws StashClientException {
    String request = MessageFormat.format(API_ONE_PR_APPROVAL, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
    delete(request, HttpURLConnection.HTTP_OK, MessageFormat.format(PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }
  
  public void postTaskOnComment(String message, Long commentId) throws StashClientException {
    String request = baseUrl + TASKS_API;
  
    JSONObject anchor = new JSONObject();
    anchor.put("id", commentId);
    anchor.put("type", "COMMENT");

    JSONObject json = new JSONObject();
    json.put("anchor", anchor);
    json.put("text", message);

    postCreate(request, json, MessageFormat.format(TASK_POST_ERROR_MESSAGE, commentId));
  }

  public void deleteTaskOnComment(StashTask task) throws StashClientException {
    String request = baseUrl + TASKS_API + "/" + task.getId();
    delete(request, MessageFormat.format(TASK_DELETION_ERROR_MESSAGE, task.getId()));
  }

  public Integer getPullRequestId(String stashProject, String stashRepository, String branchName) throws StashClientException {
    String request = MessageFormat.format(API_PULLREQUEST_BY_BRANCH, baseUrl, stashProject, stashRepository, branchName);
    JSONObject jsonPR = get(request, "Error getting pull request info");
    long size = (long)jsonPR.get("size");
    if (size != 1) {
      throw new StashClientException("Unable to find suitable pull request. Response size = " + size);
    }
    JSONArray prs = (JSONArray)jsonPR.get("values");
    JSONObject pr = (JSONObject) prs.get(0);
    return Integer.valueOf(String.valueOf(pr.get("id")));
  }

  public String getPullRequstUrl(PullRequestRef pr) {
    return MessageFormat.format("{0}/projects/{1}/repos/{2}/pull-requests/{3}", baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
  }

  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException ignored) { }
  }

  private JSONObject get(String url, String errorMessage) throws StashClientException {
      return performRequest(httpClient.prepareGet(url), null, HttpURLConnection.HTTP_OK, errorMessage);
  }

  private JSONObject post(String url, JSONObject body, String errorMessage) throws StashClientException {
    return performRequest(httpClient.preparePost(url), body, HttpURLConnection.HTTP_OK, errorMessage);
  }

  private JSONObject postCreate(String url, JSONObject body, String errorMessage) throws StashClientException {
    return performRequest(httpClient.preparePost(url), body, HttpURLConnection.HTTP_CREATED, errorMessage);
  }

  private JSONObject delete(String url, int expectedStatusCode, String errorMessage) throws StashClientException {
    return performRequest(httpClient.prepareDelete(url), null, expectedStatusCode, errorMessage);
  }

  private JSONObject delete(String url, String errorMessage) throws StashClientException {
      return delete(url, HttpURLConnection.HTTP_NO_CONTENT, errorMessage);
  }

  private JSONObject put(String url, JSONObject body, String errorMessage) throws StashClientException {
    return performRequest(httpClient.preparePut(url), body, HttpURLConnection.HTTP_OK, errorMessage);
  }

  private JSONObject performRequest(BoundRequestBuilder requestBuilder, JSONObject body, int expectedStatusCode, String errorMessage)
          throws StashClientException {
    if (body != null) {
      requestBuilder.setBody(body.toString());
    }
    Realm realm = new Realm.Builder(credentials.getLogin(), credentials.getPassword())
            .setUsePreemptiveAuth(true).setScheme(Realm.AuthScheme.BASIC).build();
    requestBuilder.setRealm(realm);
    requestBuilder.setFollowRedirect(true);
    requestBuilder.addHeader("Content-Type", "application/json");
    requestBuilder.addHeader("Accept", "application/json");

    try {
      Response response = requestBuilder.execute().get(stashTimeout, TimeUnit.MILLISECONDS);

      validateResponse(response, expectedStatusCode, errorMessage);
      return extractResponse(response);
    } catch (ExecutionException | TimeoutException | InterruptedException e) {
      throw new StashClientException(e);
    }
  }

  private static void validateResponse(Response response, int expectedStatusCode, String message) throws StashClientException {
    int responseCode = response.getStatusCode();
    if (responseCode != expectedStatusCode) {
      throw new StashClientException(message + " Received " + responseCode + ": " + formatStashApiError(response));
    }
  }

  private static JSONObject extractResponse(Response response) throws StashClientException {
    String body = null;
    body = response.getResponseBody();

    if (StringUtils.isEmpty(body)) {
      return null;
    }

    String contentType = response.getHeader("Content-Type");
    if (!JSON.match(StringUtils.strip(contentType))) {
      throw new StashClientException("Received response with type " + contentType + " instead of JSON");
    }
    try {
      Object obj = new JSONParser().parse(body);
      return (JSONObject)obj;
    } catch (ParseException | ClassCastException e) {
      throw new StashClientException("Could not parse JSON response " + e + "('" + body + "')", e);
    }
  }

  private static String formatStashApiError(Response response) throws StashClientException {
    JSONArray errors;
    JSONObject responseJson = extractResponse(response);

    errors = (JSONArray)responseJson.get("errors");

    if (errors == null) {
      throw new StashClientException("Error response did not contain an errors object '" + responseJson + "'");
    }

    List<String> errorParts = new ArrayList<>();

    for (Object o : errors) {
      try {
        JSONObject error = (JSONObject)o;
        errorParts.add((String)error.get("exceptionName") + ": " + (String)error.get("message"));
      } catch (ClassCastException e) {
        throw new StashClientException("Error response contained invalid error", e);
      }

    }

    return StringUtils.join(errorParts, ", ");
  }

  // We can't test this, as the manifest can only be loaded when deployed from a JAR-archive.
  // During unit testing this is not the case
  private static String getUserAgent(String sonarQubeVersion) {
    PluginInfo info = PluginUtils.infoForPluginClass(StashPlugin.class);
    String name;
    String version;
    name = version = "unknown";
    if (info != null) {
      name = info.getName();
      version = info.getVersion();
    }
    return MessageFormat.format("SonarQube/{0} {1}/{2} {3}",
    sonarQubeVersion == null ? "unknown" : sonarQubeVersion,
    name,
    version,
    AsyncHttpClientConfigDefaults.defaultUserAgent());
  }
  
  AsyncHttpClient createHttpClient(String sonarQubeVersion){
    return new DefaultAsyncHttpClient(
            new DefaultAsyncHttpClientConfig.Builder()
                    .setUserAgent(getUserAgent(sonarQubeVersion))
                    .setCompressionEnforced(true)
                    .build()
    );
  }
}
