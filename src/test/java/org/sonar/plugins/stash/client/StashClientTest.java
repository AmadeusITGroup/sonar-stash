package org.sonar.plugins.stash.client;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.collector.DiffReportSample;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
    when(requestBuilder.execute()).thenReturn(listenableFurture);

    httpClient = mock(AsyncHttpClient.class);
    when(httpClient.preparePost(anyString())).thenReturn(requestBuilder);
    when(httpClient.prepareGet(anyString())).thenReturn(requestBuilder);
    doNothing().when(httpClient).close();

    StashClient client = new StashClient("baseUrl", new StashCredentials("login", "password"), 1000);
    spyClient = spy(client);
    doNothing().when(spyClient).addAuthorization(requestBuilder);
    doReturn(httpClient).when(spyClient).createHttpClient();
  }

  @Test
  public void testPostCommentOnPullRequest() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);

    spyClient.postCommentOnPullRequest("Project", "Repository", "1", "Report");
    verify(requestBuilder, times(1)).execute();
    verify(httpClient, times(1)).close();
  }

  @Test
  public void testPostCommentOnPullRequestWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NOT_IMPLEMENTED);

    try {
      spyClient.postCommentOnPullRequest("Project", "Repository", "1", "Report");

      assertFalse("Wrong HTTP result should raised StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(1)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testPostCommentOnPullRequestWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));

    try {
      spyClient.postCommentOnPullRequest("Project", "Repository", "1", "Report");

      assertFalse("Exception failure should be catched and convert to StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(0)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testGetPullRequestComments() throws Exception {
    int id = 1234;
    String message = "message";
    String path = "path";
    long line = 5;

    String stashJsonComment = "{\"values\": [{\"id\": " + id + ", \"text\":\"" + message + "\", \"anchor\": {\"path\":\"" + path + "\", \"line\":" + line + "}}]}";

    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(stashJsonComment);

    StashCommentReport report = spyClient.getPullRequestComments("Project", "Repository", "1", path);

    assertTrue(report.contains(message, path, line));
    assertEquals(report.size(), 1);
    verify(httpClient, times(1)).close();
  }

  @Test
  public void testGetPullRequestCommentsWithNextPage() throws Exception {
    String path = "path";

    int id1 = 1234;
    String message1 = "message1";
    long line1 = 5;

    int id2 = 4321;
    String message2 = "message2";
    long line2 = 10;

    String stashJsonComment1 = "{\"values\": [{\"id\": " + id1 + ", \"text\":\"" + message1 + "\", \"anchor\": {\"path\":\"" + path + "\", \"line\":" + line1 + "}}],"
      + " \"isLastPage\": false, \"nextPageStart\": 1}";

    String stashJsonComment2 = "{\"values\": [{\"id\": " + id2 + ", \"text\":\"" + message2 + "\", \"anchor\": {\"path\":\"" + path + "\", \"line\":" + line2 + "}}],"
      + " \"isLastPage\": true}";

    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(stashJsonComment1, stashJsonComment2);

    StashCommentReport report = spyClient.getPullRequestComments("Project", "Repository", "1", path);
    assertTrue(report.contains(message1, path, line1));
    assertTrue(report.contains(message2, path, line2));
    assertEquals(report.size(), 2);
    verify(httpClient, times(2)).close();
  }

  @Test
  public void testGetPullRequestCommentsWithNoNextPage() throws Exception {
    String path = "path";

    int id1 = 1234;
    String message1 = "message1";
    long line1 = 5;

    int id2 = 4321;
    String message2 = "message2";
    long line2 = 10;

    String stashJsonComment1 = "{\"values\": [{\"id\": " + id1 + ", \"text\":\"" + message1 + "\", \"anchor\": {\"path\":\"" + path + "\", \"line\":" + line1 + "}}],"
      + " \"isLastPage\": true, \"nextPageStart\": 1}";

    String stashJsonComment2 = "{\"values\": [{\"id\": " + id2 + ", \"text\":\"" + message2 + "\", \"anchor\": {\"path\":\"" + path + "\", \"line\":" + line2 + "}}],"
      + " \"isLastPage\": true}";

    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(stashJsonComment1, stashJsonComment2);

    StashCommentReport report = spyClient.getPullRequestComments("Project", "Repository", "1", path);
    assertTrue(report.contains(message1, path, line1));
    assertFalse(report.contains(message2, path, line2));
    assertEquals(report.size(), 1);
    verify(httpClient, times(1)).close();
  }

  @Test
  public void testGetPullRequestCommentsWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);

    try {
      spyClient.getPullRequestComments("Project", "Repository", "1", "path");

      assertFalse("Wrong HTTP result should raised StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(1)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testGetPullRequestCommentsWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));

    try {
      spyClient.getPullRequestComments("Project", "Repository", "1", "path");

      assertFalse("Exception failure should be catched and convert to StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(0)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testGetPullRequestDiffs() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(DiffReportSample.baseReport);

    StashDiffReport report = spyClient.getPullRequestDiffs("Project", "Repository", "1");
    assertEquals(report.getDiffs().size(), 4);
    verify(httpClient, times(1)).close();
  }

  @Test
  public void testGetPullRequestDiffsWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);
    when(response.getResponseBody()).thenReturn(DiffReportSample.baseReport);

    try {
      spyClient.getPullRequestDiffs("Project", "Repository", "1");

      assertFalse("Wrong HTTP result should raised StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(1)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testGetPullRequestDiffsWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));

    try {
      spyClient.getPullRequestDiffs("Project", "Repository", "1");

      assertFalse("Exception failure should be catched and convert to StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(0)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testPostCommentLineOnPullRequest() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    StringWriter stringWriter = new StringWriter();
    IOUtils.copy(getClass().getResourceAsStream("CommentCreatedResponse.json"), stringWriter);
    when(response.getResponseBody()).thenReturn(stringWriter.toString());

    Long commentId = spyClient.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");
    assertEquals(Long.valueOf(100L), commentId);
    verify(requestBuilder, times(1)).execute();
    verify(httpClient, times(1)).close();
  }

  @Test
  public void testTaskOnComment() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);

    spyClient.postTaskOnComment("Task Message", 100L);
    verify(requestBuilder, times(1)).execute();
    verify(httpClient, times(1)).close();
  }

  @Test
  public void testPostCommentLineOnPullRequestWithWrongHTTPResult() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_FORBIDDEN);

    try {
      spyClient.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");

      assertFalse("Wrong HTTP result should raised StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(1)).getStatusText();
      verify(httpClient, times(1)).close();
    }
  }

  @Test
  public void testPostCommentLineOnPullRequestWithException() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_CREATED);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));

    try {
      spyClient.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");

      assertFalse("Exception failure should be catched and convert to StashClientException", true);

    } catch (StashClientException e) {
      verify(response, times(0)).getStatusText();
      verify(httpClient, times(1)).close();
    }

  }
}
