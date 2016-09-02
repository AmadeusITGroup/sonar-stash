package org.sonar.plugins.stash.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ning.http.client.*;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;
import org.sonar.plugins.stash.issue.collector.DiffReportSample;

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class StashClientTest {

  @Mock
  BoundRequestBuilder requestBuilder;
  
  @Mock
  Response response;
  
  @Mock
  AsyncHttpClient httpClient;
  
  @Mock
  ListenableFuture<Response> listenableFurture;
  
  @Spy
  StashClient spyClient;
   
  @Before
  public void setUp() throws Exception {
    response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NOT_FOUND);
    when(response.getStatusText()).thenReturn("Response status");
    
    listenableFurture = mock(ListenableFuture.class);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(response);
    
    requestBuilder = mock(BoundRequestBuilder.class);
    when(requestBuilder.setBody(anyString())).thenReturn(requestBuilder);
    when(requestBuilder.addHeader(anyString(), anyString())).thenReturn(requestBuilder);
    when(requestBuilder.setUrl(anyString())).thenReturn(requestBuilder);
    when(requestBuilder.execute()).thenReturn(listenableFurture);

    httpClient = mock(AsyncHttpClient.class);
    when(httpClient.prepareRequest(any(Request.class))).thenReturn(requestBuilder);
    when(httpClient.preparePost(anyString())).thenReturn(requestBuilder);
    when(httpClient.prepareGet(anyString())).thenReturn(requestBuilder);
    when(httpClient.prepareDelete(anyString())).thenReturn(requestBuilder);
    when(httpClient.preparePut(anyString())).thenReturn(requestBuilder);

    StashClient client = new StashClient("baseUrl", new StashCredentials("login", "password"), 1000);
    client.setHttpClient(httpClient);
    spyClient = spy(client);
  }
  
  @Test
  public void testPostCommentOnPullRequest() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    
    spyClient.postCommentOnPullRequest("Project", "Repository", "1", "Report");
    verify(requestBuilder, times(1)).execute();
  }
  
  @Test
  public void testPostCommentOnPullRequestWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NOT_IMPLEMENTED);
    when(response.getHeader("Content-Type")).thenReturn("application/json");
    when(response.getResponseBody()).thenReturn("{\n" +
            "    \"errors\": [\n" +
            "        {\n" +
            "            \"context\": null,\n" +
            "            \"message\": \"A detailed error message.\",\n" +
            "            \"exceptionName\": \"seriousException\"\n" +
            "        }\n" +
            "    ]\n" +
            "}");

    try {
      spyClient.postCommentOnPullRequest("Project", "Repository", "1", "Report");
    
      Assert.fail("Wrong HTTP result should raised StashClientException");
    
    } catch (StashClientException e) {
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString(
              String.valueOf(HttpURLConnection.HTTP_NOT_IMPLEMENTED)));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("detailed error"));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("seriousException"));
    }
  }
  
  @Test
  public void testPostCommentOnPullRequestWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
      
    try {
      spyClient.postCommentOnPullRequest("Project", "Repository", "1", "Report");
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
    
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testGetPullRequestComments() throws Exception {
    String stashJsonComment = "{\"values\": [{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}]}";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(stashJsonComment);
    
    StashCommentReport report = spyClient.getPullRequestComments("Project", "Repository", "1", "path");
    
    assertTrue(report.contains("message", "path", 5));
    assertEquals(report.size(), 1);
  }
  
  @Test
  public void testGetPullRequestCommentsWithNextPage() throws Exception {
    String stashJsonComment1 = "{\"values\": [{\"id\":1234, \"text\":\"message1\", \"anchor\": {\"path\":\"path\", \"line\":1},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}], \"isLastPage\": false, \"nextPageStart\": 1}";
    
    String stashJsonComment2 = "{\"values\": [{\"id\":4321, \"text\":\"message2\", \"anchor\": {\"path\":\"path\", \"line\":2},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}], \"isLastPage\": true}";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(stashJsonComment1, stashJsonComment2);
    
    StashCommentReport report = spyClient.getPullRequestComments("Project", "Repository", "1", "path");
    assertTrue(report.contains("message1", "path", 1));
    assertTrue(report.contains("message2", "path", 2));
    assertEquals(report.size(), 2);
  }
  
  @Test
  public void testGetPullRequestCommentsWithNoNextPage() throws Exception {
    String stashJsonComment1 = "{\"values\": [{\"id\":1234, \"text\":\"message1\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}], \"isLastPage\": true, \"nextPageStart\": 1}";
    
    String stashJsonComment2 = "{\"values\": [{\"id\":4321, \"text\":\"message2\", \"anchor\": {\"path\":\"path\", \"line\":10},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}], \"isLastPage\": true}";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(stashJsonComment1, stashJsonComment2);
    
    StashCommentReport report = spyClient.getPullRequestComments("Project", "Repository", "1", "path");
    assertTrue(report.contains("message1", "path", 5));
    assertFalse(report.contains("message2", "path", 10));
    assertEquals(report.size(), 1);
  }
  
  @Test
  public void testGetPullRequestCommentsWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.getPullRequestComments("Project", "Repository", "1", "path");
    
      assertFalse("Wrong HTTP result should raised StashClientException", true);
    
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testGetPullRequestCommentsWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.getPullRequestComments("Project", "Repository", "1", "path");

      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }

  @Test
  public void testGetPullRequestDiffs() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(DiffReportSample.baseReport);
    
    StashDiffReport report = spyClient.getPullRequestDiffs("Project", "Repository", "1");
    assertEquals(report.getDiffs().size(), 4);
  }
  
  @Test
  public void testGetPullRequestDiffsWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    when(response.getResponseBody()).thenReturn(DiffReportSample.baseReport);
    
    try {
      spyClient.getPullRequestDiffs("Project", "Repository", "1");
    
      assertFalse("Wrong HTTP result should raised StashClientException", true);
     
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testGetPullRequestDiffsWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
   
    try{
      spyClient.getPullRequestDiffs("Project", "Repository", "1");
    
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
    
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testPostCommentLineOnPullRequest() throws Exception {
    String stashJsonComment = "{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(response.getResponseBody()).thenReturn(stashJsonComment);
    
    StashComment comment = spyClient.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");
    
    assertEquals((long) 1234, comment.getId());
    verify(requestBuilder, times(1)).execute();
  }
  
  @Test
  public void testPostCommentLineOnPullRequestWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    when(response.getHeader("Content-Type")).thenReturn("application/json");
    when(response.getResponseBody()).thenReturn("{\n" +
            "    \"errors\": [\n" +
            "        {\n" +
            "            \"context\": null,\n" +
            "            \"message\": \"A detailed error message.\",\n" +
            "            \"exceptionName\": \"seriousException\"\n" +
            "        }\n" +
            "    ]\n" +
            "}");

    try {
      spyClient.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");
    
      Assert.fail("Wrong HTTP result should raised StashClientException");
      
    } catch (StashClientException e) {
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("detailed error"));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("seriousException"));
    }
  }
  
  @Test
  public void testPostCommentLineOnPullRequestWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");

      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }

  @Test
  public void testGetUser() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    
    String jsonUser = "{\"name\":\"SonarQube\", \"email\":\"sq@email.com\", \"id\":1, \"slug\":\"sonarqube\"}";
    when(response.getResponseBody()).thenReturn(jsonUser);
    
    StashUser user = spyClient.getUser("sonarqube");
    
    assertEquals(user.getId(), 1);
    assertEquals(user.getName(), "SonarQube");
    assertEquals(user.getEmail(), "sq@email.com");
    assertEquals(user.getSlug(), "sonarqube");
    
  }
    
  @Test
  public void testGetUserWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.getUser("sonarqube");
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testGetUserWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.getUser("sonarqube");
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
    
  @Test
  public void testDeletePullRequestComment() throws Exception {
    StashComment stashComment = mock(StashComment.class);
    when(stashComment.getId()).thenReturn((long) 1234);
    when(stashComment.getVersion()).thenReturn((long) 0);
        
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);
    
    spyClient.deletePullRequestComment("Project", "Repository", "1", stashComment);
    verify(requestBuilder, times(1)).execute();
  }
  
  @Test
  public void testDeletePullRequestCommentWithWrongHTTPResult() throws Exception {
    StashComment stashComment = mock(StashComment.class);
    when(stashComment.getId()).thenReturn((long) 1234);
    when(stashComment.getVersion()).thenReturn((long) 0);
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.deletePullRequestComment("Project", "Repository", "1",  stashComment);
    
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testGetPullRequest() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    
    String jsonPullRequest = "{\"version\": 1, \"title\":\"PR-Test\", \"description\":\"PR-test\", \"reviewers\": []}";
    when(response.getResponseBody()).thenReturn(jsonPullRequest);
    
    StashPullRequest pullRequest = spyClient.getPullRequest("Project", "Repository", "123");
    
    assertEquals(pullRequest.getId(), "123");
    assertEquals(pullRequest.getProject(), "Project");
    assertEquals(pullRequest.getRepository(), "Repository");
    assertEquals(pullRequest.getVersion(), 1);
  }
    
  @Test
  public void testGetPullRequestWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.getPullRequest("Project", "Repository", "123");
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testGetPullRequestWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.getPullRequest("Project", "Repository", "123");
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testApprovePullRequest() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    
    spyClient.approvePullRequest("Project", "Repository", "123");
    
    verify(requestBuilder, times(1)).execute();
  }
    
  @Test
  public void testApprovePullRequestWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.approvePullRequest("Project", "Repository", "123");
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testApprovePullRequestWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.approvePullRequest("Project", "Repository", "123");
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
    
  @Test
  public void testResetPullRequestApproval() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    
    spyClient.resetPullRequestApproval("Project", "Repository", "123");
    
    verify(requestBuilder, times(1)).execute();
  }
    
  @Test
  public void testResetPullRequestApprovalWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.resetPullRequestApproval("Project", "Repository", "123");
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testDeletePullRequestCommentWithException() throws Exception {
    StashComment stashComment = mock(StashComment.class);
    when(stashComment.getId()).thenReturn((long) 1234);
    when(stashComment.getVersion()).thenReturn((long) 0);
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.deletePullRequestComment("Project", "Repository", "1",  stashComment);
  
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testResetPullRequestApprovalWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.resetPullRequestApproval("Project", "Repository", "123");
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testAddPullRequestReviewer() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    
    StashUser stashUser = mock(StashUser.class);
    when(stashUser.getName()).thenReturn("sonarqube");
    
    ArrayList<StashUser> reviewers = new ArrayList<>();
    reviewers.add(stashUser);
    
    spyClient.addPullRequestReviewer("Project", "Repository", "123", (long) 1, reviewers);
    
    verify(requestBuilder, times(1)).execute();
  }
  
  @Test
  public void testAddPullRequestReviewerWithNoReviewer() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    
    spyClient.addPullRequestReviewer("Project", "Repository", "123", (long) 1, new ArrayList<StashUser>());
    
    verify(requestBuilder, times(1)).execute();
  }
    
  @Test
  public void testAddPullRequestReviewerWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    StashUser stashUser = mock(StashUser.class);
    when(stashUser.getName()).thenReturn("sonarqube");
    
    ArrayList<StashUser> reviewers = new ArrayList<>();
    reviewers.add(stashUser);
    
    try {
      spyClient.addPullRequestReviewer("Project", "Repository", "123", (long) 1, reviewers);
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testAddPullRequestReviewerWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    StashUser stashUser = mock(StashUser.class);
    when(stashUser.getName()).thenReturn("sonarqube");
    
    ArrayList<StashUser> reviewers = new ArrayList<>();
    reviewers.add(stashUser);
    
    try {
      spyClient.addPullRequestReviewer("Project", "Repository", "123", (long) 1, reviewers);
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testPostTaskOnComment() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    
    spyClient.postTaskOnComment("message", (long) 1111);
    
    verify(requestBuilder, times(1)).execute();
  }
  
  @Test
  public void testPostTaskOnCommentWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.postTaskOnComment("message", (long) 1111);
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testPostTaskOnCommentWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.postTaskOnComment("message", (long) 1111);
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testDeleteTaskOnComment() throws Exception {
    StashTask task = mock(StashTask.class);
    when(task.getId()).thenReturn((long) 1111);
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);
    
    spyClient.deleteTaskOnComment(task);
    
    verify(requestBuilder, times(1)).execute();
  }
  
  @Test
  public void testDeleteTaskOnCommentWithWrongHTTPResult() throws Exception {
    StashTask task = mock(StashTask.class);
    when(task.getId()).thenReturn((long) 1111);
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    
    try {
      spyClient.deleteTaskOnComment(task);
      
      assertFalse("Wrong HTTP result should raised StashClientException", true);
      
    } catch (StashClientException e) { }
  }
  
  @Test
  public void testDeleteTaskOnCommentWithException() throws Exception {
    StashTask task = mock(StashTask.class);
    when(task.getId()).thenReturn((long) 1111);
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      spyClient.deleteTaskOnComment(task);
      
      assertFalse("Exception failure should be catched and convert to StashClientException", true);
      
    } catch (StashClientException e) { }
  }

  @Test
  public void testClosingStashClientClosesHttpClient() {
    verify(httpClient, times(0)).close();
    spyClient.close();
    verify(httpClient, times(1)).close();
  }
}
