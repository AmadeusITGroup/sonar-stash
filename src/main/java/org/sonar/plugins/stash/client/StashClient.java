package org.sonar.plugins.stash.client;

import org.asynchttpclient.*;
import org.asynchttpclient.config.AsyncHttpClientConfigDefaults;
import org.json.simple.DeserializationException;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.sonar.plugins.stash.PeekableInputStream;
import org.sonar.plugins.stash.PluginInfo;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.StashPlugin.IssueType;
import org.sonar.plugins.stash.StashPluginUtils;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.*;
import org.sonar.plugins.stash.issue.collector.StashCollector;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StashClient implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(StashClient.class);
  private static final String MDC_URL_KEY = "url";
  private static final String MDC_METHOD_KEY = "method";

  private final String baseUrl;
  private final StashCredentials credentials;
  private final int stashTimeout;
  private AsyncHttpClient httpClient;

  private static final String REST_API = "/rest/api/1.0/";
  private static final String USER_API = "{0}" + REST_API + "users/{1}";
  private static final String REPO_API = "{0}" + REST_API + "projects/{1}/repos/{2}/";
  private static final String TASKS_API = REST_API + "tasks";

  private static final String API_ALL_PR = REPO_API + "pull-requests/";
  private static final String API_ONE_PR = API_ALL_PR + "{3,number,#}";

  private static final String API_ONE_PR_ALL_COMMENTS = API_ONE_PR + "/comments";
  private static final String API_ONE_PR_DIFF = API_ONE_PR + "/diff?withComments=true";
  private static final String API_ONE_PR_APPROVAL = API_ONE_PR + "/approve";
  private static final String API_ONE_PR_COMMENT_PATH = API_ONE_PR + "/comments?path={4}&start={5,number,#}";

  private static final String API_ONE_PR_ONE_COMMENT = API_ONE_PR_ALL_COMMENTS + "/{4}?version={5}";

  private static final String PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE = "Unable to change status of pull-request {0}"
                                                                       + " #{1,number,#}.";
  private static final String PULL_REQUEST_GET_ERROR_MESSAGE = "Unable to retrieve pull-request {0} #{1,number,#}.";
  private static final String PULL_REQUEST_PUT_ERROR_MESSAGE = "Unable to update pull-request {0} #{1,number,#}.";
  private static final String USER_GET_ERROR_MESSAGE = "Unable to retrieve user {0}.";
  private static final String COMMENT_POST_ERROR_MESSAGE = "Unable to post a comment to {0} #{1,number,#}.";
  private static final String COMMENT_GET_ERROR_MESSAGE = "Unable to get comment linked to {0} #{1,number,#}.";
  private static final String COMMENT_DELETION_ERROR_MESSAGE = "Unable to delete comment {0,number,#}"
                                                             + " from pull-request {1} #{2,number,#}.";
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

  public void postCommentOnPullRequest(PullRequestRef pr, String report) {

    String request = MessageFormat.format(API_ONE_PR_ALL_COMMENTS,
                                          baseUrl,
                                          pr.project(),
                                          pr.repository(),
                                          pr.pullRequestId());
    JsonObject json = new JsonObject();
    json.put("text", report);

    postCreate(request, json, MessageFormat.format(COMMENT_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }

  public StashCommentReport getPullRequestComments(PullRequestRef pr, String path) {
    StashCommentReport result = new StashCommentReport();

    long start = 0;
    boolean isLastPage = false;

    while (!isLastPage) {
      String request = MessageFormat.format(API_ONE_PR_COMMENT_PATH,
          baseUrl,
          pr.project(),
          pr.repository(),
          pr.pullRequestId(),
          path,
          start);
      JsonObject jsonComments = get(request,
          MessageFormat.format(COMMENT_GET_ERROR_MESSAGE,
              pr.repository(),
              pr.pullRequestId()));
      result.add(StashCollector.extractComments(jsonComments));

      // Stash pagination: check if you get all comments linked to the pull-request
      isLastPage = StashCollector.isLastPage(jsonComments);
      start = StashCollector.getNextPageStart(jsonComments);
    }

    return result;
  }

  public void deletePullRequestComment(PullRequestRef pr, StashComment comment) {

    String request = MessageFormat.format(API_ONE_PR_ONE_COMMENT,
                                          baseUrl,
                                          pr.project(),
                                          pr.repository(),
                                          pr.pullRequestId(),
                                          Long.toString(comment.getId()),
                                          Long.toString(comment.getVersion()));

    delete(request,
           MessageFormat.format(COMMENT_DELETION_ERROR_MESSAGE, comment.getId(), pr.repository(), pr.pullRequestId()));
  }

  public StashDiffReport getPullRequestDiffs(PullRequestRef pr) {
    String request = MessageFormat.format(API_ONE_PR_DIFF,
        baseUrl,
        pr.project(),
        pr.repository(),
        pr.pullRequestId());
    JsonObject jsonDiffs = get(request,
        MessageFormat.format(COMMENT_GET_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
    return StashCollector.extractDiffs(jsonDiffs);
  }

  public StashComment postCommentLineOnPullRequest(PullRequestRef pr,
                                                   String message,
                                                   String path,
                                                   long line,
                                                   IssueType type) {
    String request = MessageFormat.format(API_ONE_PR_ALL_COMMENTS,
                                          baseUrl,
                                          pr.project(),
                                          pr.repository(),
                                          pr.pullRequestId());

    JsonObject anchor = new JsonObject();
    if (line != 0L) {
      anchor.put("line", line);
      anchor.put("lineType", type.name());
    }

    String fileType = "TO";
    if (type == IssueType.CONTEXT) {
      fileType = "FROM";
    }
    anchor.put("fileType", fileType);

    anchor.put("path", path);

    JsonObject json = new JsonObject();
    json.put("text", message);
    json.put("anchor", anchor);

    JsonObject response = postCreate(request, json,
                                     MessageFormat.format(COMMENT_POST_ERROR_MESSAGE,
                                                          pr.repository(),
                                                          pr.pullRequestId()));

    return StashCollector.extractComment(response, path, line);
  }

  public StashUser getUser(String userSlug) {

    String request = MessageFormat.format(USER_API, baseUrl, userSlug);
    JsonObject response = get(request, MessageFormat.format(USER_GET_ERROR_MESSAGE, userSlug));

    return StashCollector.extractUser(response);
  }

  public StashPullRequest getPullRequest(PullRequestRef pr) {
    String request = MessageFormat.format(API_ONE_PR, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());
    JsonObject response = get(request,
                              MessageFormat.format(PULL_REQUEST_GET_ERROR_MESSAGE,
                                                   pr.repository(),
                                                   pr.pullRequestId()));

    return StashCollector.extractPullRequest(pr, response);
  }

  public void addPullRequestReviewer(PullRequestRef pr, long pullRequestVersion, List<StashUser> reviewers) {
    String request = MessageFormat.format(API_ONE_PR, baseUrl, pr.project(), pr.repository(), pr.pullRequestId());

    JsonObject json = new JsonObject();

    JsonArray jsonReviewers = new JsonArray();
    for (StashUser reviewer : reviewers) {
      JsonObject reviewerName = new JsonObject();
      reviewerName.put("name", reviewer.getName());

      JsonObject user = new JsonObject();
      user.put("user", reviewerName);

      jsonReviewers.add(user);
    }

    json.put("reviewers", jsonReviewers);
    json.put("id", pr.pullRequestId());
    json.put("version", pullRequestVersion);

    put(request, json, MessageFormat.format(PULL_REQUEST_PUT_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }

  public void approvePullRequest(PullRequestRef pr) {
    String request = MessageFormat.format(API_ONE_PR_APPROVAL,
                                          baseUrl,
                                          pr.project(),
                                          pr.repository(),
                                          pr.pullRequestId());
    post(request,
         null,
         MessageFormat.format(PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }

  public void resetPullRequestApproval(PullRequestRef pr) {
    String request = MessageFormat.format(API_ONE_PR_APPROVAL,
                                          baseUrl,
                                          pr.project(),
                                          pr.repository(),
                                          pr.pullRequestId());
    delete(request,
           HttpURLConnection.HTTP_OK,
           MessageFormat.format(PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE, pr.repository(), pr.pullRequestId()));
  }

  public void postTaskOnComment(String message, Long commentId) {
    String request = baseUrl + TASKS_API;

    JsonObject anchor = new JsonObject();
    anchor.put("id", commentId);
    anchor.put("type", "COMMENT");

    JsonObject json = new JsonObject();
    json.put("anchor", anchor);
    json.put("text", message);

    postCreate(request, json, MessageFormat.format(TASK_POST_ERROR_MESSAGE, commentId));
  }

  public void deleteTaskOnComment(StashTask task) {
    String request = baseUrl + TASKS_API + "/" + task.getId();
    delete(request, MessageFormat.format(TASK_DELETION_ERROR_MESSAGE, task.getId()));
  }

  @Override
  public void close() {
    try {
      httpClient.close();
    } catch (IOException e) {
      LOGGER.debug("Ignoring exception while closing StashClient: {}", e, e);
    }
  }

  private JsonObject get(String url, String errorMessage) {
    return performRequest(httpClient.prepareGet(url), null, HttpURLConnection.HTTP_OK, errorMessage);
  }

  private JsonObject post(String url, JsonObject body, String errorMessage) {
    return performRequest(httpClient.preparePost(url), body, HttpURLConnection.HTTP_OK, errorMessage);
  }

  private JsonObject postCreate(String url, JsonObject body, String errorMessage) {
    return performRequest(httpClient.preparePost(url), body, HttpURLConnection.HTTP_CREATED, errorMessage);
  }

  private JsonObject delete(String url, int expectedStatusCode, String errorMessage) {
    return performRequest(httpClient.prepareDelete(url), null, expectedStatusCode, errorMessage);
  }

  private JsonObject delete(String url, String errorMessage) {
    return delete(url, HttpURLConnection.HTTP_NO_CONTENT, errorMessage);
  }

  private JsonObject put(String url, JsonObject body, String errorMessage) {
    return performRequest(httpClient.preparePut(url), body, HttpURLConnection.HTTP_OK, errorMessage);
  }

  private void addAuth(BoundRequestBuilder requestBuilder) {
    if (credentials != null && credentials.getLogin() != null) {
      String password = Optional.ofNullable(credentials.getPassword()).orElse("");
      Realm realm = new Realm.Builder(credentials.getLogin(), password).setUsePreemptiveAuth(true)
          .setScheme(Realm.AuthScheme.BASIC).build();
      requestBuilder.setRealm(realm);
    }
  }

  private JsonObject performRequest(BoundRequestBuilder requestBuilder,
      JsonObject body,
      int expectedStatusCode,
      String errorMessage) {

    if (body != null) {
      requestBuilder.setBody(body.toJson());
    }
    addAuth(requestBuilder);
    requestBuilder.setFollowRedirect(true);
    requestBuilder.addHeader("Content-Type", JSON.toString());
    requestBuilder.addHeader("Accept", JSON.toString());
    requestBuilder.setCharset(StandardCharsets.UTF_8);

    Request request = requestBuilder.build();
    MDC.put(MDC_URL_KEY, request.getUrl());
    MDC.put(MDC_METHOD_KEY, request.getMethod());

    try {
      Response response = httpClient.executeRequest(request).get(stashTimeout, TimeUnit.MILLISECONDS);
      validateResponse(response, expectedStatusCode, errorMessage);
      return extractResponse(response);

    } catch (ExecutionException | TimeoutException e) {
      throw new StashClientException(request.getUrl(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StashClientException(request.getUrl(), e);
    } finally {
      MDC.remove(MDC_URL_KEY);
      MDC.remove(MDC_METHOD_KEY);
    }
  }

  private static void validateResponse(Response response, int expectedStatusCode, String message) {

    int responseCode = response.getStatusCode();
    if (responseCode != expectedStatusCode) {
      throw new StashClientException(message + " Received " + responseCode + ": " + formatStashApiError(response));
    }
  }

  private static JsonObject extractResponse(Response response) {
    PeekableInputStream bodyStream = new PeekableInputStream(response.getResponseBodyAsStream());

    try {
      if (!bodyStream.peek().isPresent()) {
        return null;
      }
      String contentType = response.getHeader("Content-Type");
      if (!JSON.match(contentType.trim())) {
        throw new StashClientException("Received response with type " + contentType + " instead of JSON");
      }
      Reader body = new InputStreamReader(bodyStream);
      Object obj = Jsoner.deserialize(body);
      return (JsonObject)obj;
    } catch (DeserializationException | ClassCastException | IOException e) {
      throw new StashClientException("Could not parse JSON response", e);
    }
  }

  private static String formatStashApiError(Response response) {

    JsonObject responseJson = extractResponse(response);

    // squid:S2259: making sure that we do not have a null value that would make a NullPointerException when used
    if (responseJson == null) {
      throw new StashClientException("The responseJson could not be extracted from the response !");
    }

    JsonArray errors = (JsonArray)responseJson.get("errors");

    if (errors == null) {
      throw new StashClientException("Error response did not contain an errors object '" + responseJson + "'");
    }

    List<String> errorParts = new ArrayList<>();

    for (Object o : errors) {
      try {
        JsonObject error = (JsonObject)o;
        errorParts.add((String)error.get("exceptionName") + ": " + (String)error.get("message"));
      } catch (ClassCastException e) {
        throw new StashClientException("Error response contained invalid error", e);
      }

    }

    return String.join(", ", errorParts);
  }

  // We can't test this, as the manifest can only be loaded when deployed from a JAR-archive.
  // During unit testing this is not the case
  private static String getUserAgent(String sonarQubeVersion) {
    PluginInfo info = StashPluginUtils.getPluginInfo();
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

  AsyncHttpClient createHttpClient(String sonarQubeVersion) {
    return new DefaultAsyncHttpClient(
        new DefaultAsyncHttpClientConfig.Builder()
            .setUserAgent(getUserAgent(sonarQubeVersion))
            .setCompressionEnforced(true)
            .setUseProxySelector(true)
            .setUseProxyProperties(true)
            .build()
    );
  }
}
