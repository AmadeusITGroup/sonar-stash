package org.sonar.plugins.stash.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;

public class StashClient implements AutoCloseable {

  private final String baseUrl;
  private final StashCredentials credentials;
  private final int stashTimeout;
  private AsyncHttpClient httpClient;

  private static final String REST_API = "/rest/api/1.0/";
  
  private static final String USER_API = "{0}users/{1}";
  private static final String REPO_API = "{0}projects/{1}/repos/{2}/";
  private static final String PULL_REQUESTS_API = REPO_API + "pull-requests/";
  private static final String PULL_REQUEST_API = PULL_REQUESTS_API + "{3}";
  private static final String COMMENTS_PULL_REQUEST_API = PULL_REQUEST_API + "/comments";
  private static final String COMMENT_PULL_REQUEST_API = COMMENTS_PULL_REQUEST_API + "/{4}?version={5}";
  private static final String DIFF_PULL_REQUEST_API = PULL_REQUEST_API + "/diff";
  private static final String APPROVAL_PULL_REQUEST_API = PULL_REQUEST_API + "/approve";
  private static final String TASKS_API = REST_API + "tasks";
  
  private static final String PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE = "Unable to change status of pull-request {0} #{1}. Received {2} with message {3}.";  
  private static final String PULL_REQUEST_GET_ERROR_MESSAGE = "Unable to retrieve pull-request {0} #{1}. Received {2} with message {3}.";  
  private static final String PULL_REQUEST_PUT_ERROR_MESSAGE = "Unable to update pull-request {0} #{1}. Received {2} with message {3}.";  
  private static final String USER_GET_ERROR_MESSAGE = "Unable to retrieve user {0}. Received {1} with message {2}.";  
  private static final String COMMENT_POST_ERROR_MESSAGE = "Unable to post a comment to {0} #{1}. Received {2} with message {3}.";  
  private static final String COMMENT_GET_ERROR_MESSAGE = "Unable to get comment linked to {0} #{1}. Received {2} with message {3}.";  
  private static final String COMMENT_DELETION_ERROR_MESSAGE = "Unable to delete comment {0} from pull-request {1} #{2}. Received {3} with message {4}.";  
  private static final String TASK_POST_ERROR_MESSAGE = "Unable to post a task on comment {0}. Received {1} with message {2}.";  
  private static final String TASK_DELETION_ERROR_MESSAGE = "Unable to delete task {0}. Received {1} with message {2}.";

  public StashClient(String url, StashCredentials credentials, int stashTimeout) {
    this.baseUrl = url;
    this.credentials = credentials;
    this.stashTimeout = stashTimeout;
    this.httpClient = createHttpClient();
  }

  public void postCommentOnPullRequest(String project, String repository, String pullRequestId, String report)
      throws StashClientException {

    String request = MessageFormat.format(COMMENTS_PULL_REQUEST_API, baseUrl + REST_API, project, repository, pullRequestId);
    JSONObject json = new JSONObject();
    json.put("text", report);

    BoundRequestBuilder requestBuilder = httpClient.preparePost(request);
    requestBuilder.setBody(json.toString());

    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_CREATED) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(COMMENT_POST_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    }
  }

  public StashCommentReport getPullRequestComments(String project, String repository, String pullRequestId, String path)
      throws StashClientException {
    StashCommentReport result = new StashCommentReport();

    long start = 0;
    boolean isLastPage = false; 
    
    while (! isLastPage){
      try {
        String request = MessageFormat.format(COMMENTS_PULL_REQUEST_API + "?path={4}&start={5}", baseUrl + REST_API, project, repository, pullRequestId, path, start);
        BoundRequestBuilder requestBuilder = httpClient.prepareGet(request);

        Response response = executeRequest(requestBuilder);
        int responseCode = response.getStatusCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
          String responseMessage = response.getStatusText();
          throw new StashClientException(MessageFormat.format(COMMENT_GET_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
        } else{
          String jsonComments = response.getResponseBody();
          result.add(StashCollector.extractComments(jsonComments));
            
          // Stash pagination: check if you get all comments linked to the pull-request
          isLastPage = StashCollector.isLastPage(jsonComments);
          start = StashCollector.getNextPageStart(jsonComments);
        }
      } catch (ExecutionException | TimeoutException | InterruptedException | StashReportExtractionException | IOException e) {
        throw new StashClientException(e);
      } finally{
        httpClient.close();
      }
    }
  
    return result;
  } 
  
  public void deletePullRequestComment(String project, String repository, String pullRequestId, StashComment comment)
      throws StashClientException {

    String request = MessageFormat.format(COMMENT_PULL_REQUEST_API, baseUrl + REST_API,
                      project, repository, pullRequestId, Long.toString(comment.getId()), Long.toString(comment.getVersion()));

    BoundRequestBuilder requestBuilder = httpClient.prepareDelete(request);
    
    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(COMMENT_DELETION_ERROR_MESSAGE, comment.getId(), repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    }
  }
  
  public StashDiffReport getPullRequestDiffs(String project, String repository, String pullRequestId)
      throws StashClientException {
    StashDiffReport result = new StashDiffReport();
    
    try {
      String request = MessageFormat.format(DIFF_PULL_REQUEST_API + "?withComments=true", baseUrl + REST_API, project, repository, pullRequestId);
      BoundRequestBuilder requestBuilder = httpClient.prepareGet(request);
        
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(COMMENT_GET_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      } else{
        String jsonDiffs = response.getResponseBody();
        result = StashCollector.extractDiffs(jsonDiffs);
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | StashReportExtractionException | IOException e) {
      throw new StashClientException(e);
    }
  
    return result;
  } 
  
  public StashComment postCommentLineOnPullRequest(String project, String repository, String pullRequestId, String message, String path, long line, String type)
      throws StashClientException {
    StashComment result = null;
    
    String request = MessageFormat.format(COMMENTS_PULL_REQUEST_API, baseUrl + REST_API, project, repository,
        pullRequestId);

    JSONObject anchor = new JSONObject();
    anchor.put("line", line);
    anchor.put("lineType", type);
    
    String fileType = "TO";
    if (StringUtils.equals(type, StashPlugin.CONTEXT_ISSUE_TYPE)){
      fileType = "FROM";
    }
    anchor.put("fileType", fileType);
    
    anchor.put("path", path);
    
    JSONObject json = new JSONObject();
    json.put("text", message);
    json.put("anchor", anchor);

    BoundRequestBuilder requestBuilder = httpClient.preparePost(request);
    requestBuilder.setBody(json.toString());
    
    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_CREATED) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(COMMENT_POST_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
      
      // get generated comment
      result = StashCollector.extractComment(response.getResponseBody(), path, line);
      
    } catch (ExecutionException | TimeoutException | IOException | InterruptedException | StashReportExtractionException e) {
      throw new StashClientException(e);
    }
    
    return result;
  }
  
  public StashUser getUser(String userSlug)
      throws StashClientException {
    
    try {
      String request = MessageFormat.format(USER_API, baseUrl + REST_API, userSlug);
      BoundRequestBuilder requestBuilder = httpClient.prepareGet(request);

      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(USER_GET_ERROR_MESSAGE, userSlug, responseCode, responseMessage));
      } else {
        String jsonUser = response.getResponseBody();
        return StashCollector.extractUser(jsonUser);
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | StashReportExtractionException | IOException e) {
      throw new StashClientException(e);
    }
  }
  
  public StashPullRequest getPullRequest(String project, String repository, String pullRequestId)
      throws StashClientException {
    
    try {
      String request = MessageFormat.format(PULL_REQUEST_API, baseUrl + REST_API, project, repository, pullRequestId);
      BoundRequestBuilder requestBuilder = httpClient.prepareGet(request);

      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(PULL_REQUEST_GET_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      } else {
        String jsonPullRequest = response.getResponseBody();
        return StashCollector.extractPullRequest(project, repository, pullRequestId, jsonPullRequest);
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | StashReportExtractionException | IOException e) {
      throw new StashClientException(e);
    }
  }
  
  public void addPullRequestReviewer(String project, String repository, String pullRequestId, long pullRequestVersion, ArrayList<StashUser> reviewers)
      throws StashClientException {
    String request = MessageFormat.format(PULL_REQUEST_API, baseUrl + REST_API, project, repository, pullRequestId);

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
    json.put("id", pullRequestId);
    json.put("version", pullRequestVersion);

    BoundRequestBuilder requestBuilder = httpClient.preparePut(request);
    requestBuilder.setBody(json.toString());

    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(PULL_REQUEST_PUT_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    }
  }

  public void approvePullRequest(String project, String repository, String pullRequestId) throws StashClientException {
    String request = MessageFormat.format(APPROVAL_PULL_REQUEST_API, baseUrl + REST_API, project, repository, pullRequestId);
    
    BoundRequestBuilder requestBuilder = httpClient.preparePost(request);
    
    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    }
  }
  
  public void resetPullRequestApproval(String project, String repository, String pullRequestId) throws StashClientException {
    String request = MessageFormat.format(APPROVAL_PULL_REQUEST_API, baseUrl + REST_API, project, repository, pullRequestId);
    
    BoundRequestBuilder requestBuilder = httpClient.prepareDelete(request);
    
    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(PULL_REQUEST_APPROVAL_POST_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    }
  }
  
  public void postTaskOnComment(String message, Long commentId) throws StashClientException {
    String request = baseUrl + TASKS_API;
  
    JSONObject anchor = new JSONObject();
    anchor.put("id", commentId);
    anchor.put("type", "COMMENT");

    JSONObject json = new JSONObject();
    json.put("anchor", anchor);
    json.put("text", message);

    try {

      BoundRequestBuilder requestBuilder = httpClient.preparePost(request);
      requestBuilder.setBody(json.toString());

      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_CREATED) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(TASK_POST_ERROR_MESSAGE, commentId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | IOException | InterruptedException e) {
      throw new StashClientException(e);
    }
  }

  public void deleteTaskOnComment(StashTask task) throws StashClientException {
    String request = baseUrl + TASKS_API + "/" + task.getId();

    BoundRequestBuilder requestBuilder = httpClient.prepareDelete(request);
    
    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(TASK_DELETION_ERROR_MESSAGE, task.getId(), responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    }
  }
  
  Response executeRequest(final BoundRequestBuilder requestBuilder) throws InterruptedException, IOException,
      ExecutionException, TimeoutException {
    addAuthorization(requestBuilder);
    requestBuilder.addHeader("Content-Type", "application/json");
    return requestBuilder.execute().get(stashTimeout, TimeUnit.MILLISECONDS);
  }

  void addAuthorization(final BoundRequestBuilder requestBuilder) {
    Realm realm = new Realm.RealmBuilder().setPrincipal(credentials.getLogin()).setPassword(credentials.getPassword())
        .setUsePreemptiveAuth(true).setScheme(AuthScheme.BASIC).build();
    requestBuilder.setRealm(realm);
  }
  
  AsyncHttpClient createHttpClient(){
    return new AsyncHttpClient();
  }

  void setHttpClient(AsyncHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public void close() {
    httpClient.close();
  }
}
