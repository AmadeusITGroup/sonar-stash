package org.sonar.plugins.stash.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashReportExtractionException;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.collector.StashCollector;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Response;

public class StashClient {

  private final String baseUrl;
  private final StashCredentials credentials;
  private final int stashTimeout;

  private static final String REST_API = "/rest/api/1.0/";
  private static final String REPO_API = "{0}projects/{1}/repos/{2}/";
  private static final String PULL_REQUESTS_API = REPO_API + "pull-requests/";
  private static final String PULL_REQUEST_API = PULL_REQUESTS_API + "{3}";
  private static final String COMMENTS_PULL_REQUEST_API = PULL_REQUEST_API + "/comments";
  private static final String DIFF_PULL_REQUEST_API = PULL_REQUEST_API + "/diff";
  
  private static final String CONNECTION_POST_ERROR_MESSAGE = "Unable to post a comment to {0} #{1}. Received {2} with message {3}.";  
  private static final String CONNECTION_GET_ERROR_MESSAGE = "Unable to get comment linked to {0} #{1}. Received {2} with message {3}.";  
  
  public StashClient(String url, StashCredentials credentials, int stashTimeout) {
    this.baseUrl = url;
    this.credentials = credentials;
    this.stashTimeout = stashTimeout;
  }

  public void postCommentOnPullRequest(String project, String repository, String pullRequestId, String report)
      throws StashClientException {

    String request = MessageFormat.format(COMMENTS_PULL_REQUEST_API, baseUrl + REST_API, project, repository, pullRequestId);
    JSONObject json = new JSONObject();
    json.put("text", report);

    AsyncHttpClient httpClient = createHttpClient();
    BoundRequestBuilder requestBuilder = httpClient.preparePost(request);
    requestBuilder.setBody(json.toString());

    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_CREATED) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(CONNECTION_POST_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException e) {
      throw new StashClientException(e);
    } finally{
      httpClient.close();
    }
  }

  public StashCommentReport getPullRequestComments(String project, String repository, String pullRequestId, String path)
      throws StashClientException {
    StashCommentReport result = new StashCommentReport();
    
    AsyncHttpClient httpClient = createHttpClient();
    
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
          throw new StashClientException(MessageFormat.format(CONNECTION_GET_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
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
  
  public StashDiffReport getPullRequestDiffs(String project, String repository, String pullRequestId)
      throws StashClientException {
    StashDiffReport result = new StashDiffReport();
    
    AsyncHttpClient httpClient = createHttpClient();
    
    try {
      String request = MessageFormat.format(DIFF_PULL_REQUEST_API + "?withComments=true", baseUrl + REST_API, project, repository, pullRequestId);
      BoundRequestBuilder requestBuilder = httpClient.prepareGet(request);
        
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(CONNECTION_GET_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      } else{
        String jsonDiffs = response.getResponseBody();
        result = StashCollector.extractDiffs(jsonDiffs);
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | StashReportExtractionException | IOException e) {
      throw new StashClientException(e);
    } finally{
      httpClient.close();
    }
  
    return result;
  } 
  
  public void postCommentLineOnPullRequest(String project, String repository, String pullRequestId, String message, String path, long line, String type)
      throws StashClientException {

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

    AsyncHttpClient httpClient = createHttpClient();
    BoundRequestBuilder requestBuilder = httpClient.preparePost(request);
    requestBuilder.setBody(json.toString());
    
    try {
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_CREATED) {
        String responseMessage = response.getStatusText();
        throw new StashClientException(MessageFormat.format(CONNECTION_POST_ERROR_MESSAGE, repository, pullRequestId, responseCode, responseMessage));
      }
    } catch (ExecutionException | TimeoutException | IOException | InterruptedException e) {
      throw new StashClientException(e);
    } finally{
      httpClient.close();
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
}
