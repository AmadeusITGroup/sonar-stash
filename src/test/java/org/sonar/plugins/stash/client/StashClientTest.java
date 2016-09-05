package org.sonar.plugins.stash.client;

import com.github.tomakehurst.wiremock.client.*;
import com.github.tomakehurst.wiremock.core.*;
import com.github.tomakehurst.wiremock.junit.*;
import org.hamcrest.*;
import org.junit.*;
import org.sonar.plugins.stash.*;
import org.sonar.plugins.stash.exceptions.*;
import org.sonar.plugins.stash.issue.*;
import org.sonar.plugins.stash.issue.collector.*;

import java.net.*;
import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.net.HttpURLConnection.*;
import static org.junit.Assert.*;

public class StashClientTest extends StashTest {
  private static final int timeout = 150;
  private static final int errorTimeout = timeout + 10;

  @Rule
  public WireMockRule wireMock = new WireMockRule(new WireMockConfiguration().dynamicPort());

  StashClient client;
  StashUser testUser = new StashUser(1, "userName", "userSlug", "email");
   
  @Before
  public void setUp() throws Exception {
    primeWireMock();
    client = new StashClient("http://127.0.0.1:" + wireMock.port(),
            new StashCredentials("login", "password"),
            timeout);
  }

  @Test
  public void testPostCommentOnPullRequest() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HttpURLConnection.HTTP_CREATED)));

    client.postCommentOnPullRequest("Project", "Repository", "1", "Report");
  }

  @Test
  public void testPostCommentOnPullRequestWithWrongHTTPResult() throws Exception {
    addErrorResponse(any(anyUrl()), HTTP_NOT_IMPLEMENTED);

    try {
      client.postCommentOnPullRequest("Project", "Repository", "1", "Report");
    
      Assert.fail("Wrong HTTP result should raised StashClientException");
    
    } catch (StashClientException e) {
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString(
              String.valueOf(HttpURLConnection.HTTP_NOT_IMPLEMENTED)));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("detailed error"));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("seriousException"));
    }
  }
  
  @Test(expected = StashClientException.class)
  public void testPostCommentOnPullRequestWithException() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn( aJsonResponse().withFixedDelay(errorTimeout)));

    client.postCommentOnPullRequest("Project", "Repository", "1", "Report");
  }
  
  @Test
  public void testGetPullRequestComments() throws Exception {
    String stashJsonComment = "{\"values\": [{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}]}";

    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withBody(stashJsonComment)));

    StashCommentReport report = client.getPullRequestComments("Project", "Repository", "1", "path");
    
    assertTrue(report.contains("message", "path", 5));
    assertEquals(report.size(), 1);
  }
  
  @Test
  public void testGetPullRequestCommentsWithNextPage() throws Exception {
    String stashJsonComment1 = "{\"values\": [{\"id\":1234, \"text\":\"message1\", \"anchor\": {\"path\":\"path\", \"line\":1},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}], \"isLastPage\": false, \"nextPageStart\": 1}";
    
    String stashJsonComment2 = "{\"values\": [{\"id\":4321, \"text\":\"message2\", \"anchor\": {\"path\":\"path\", \"line\":2},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}], \"isLastPage\": true}";

    wireMock.stubFor(get(
            urlPathEqualTo("/rest/api/1.0/projects/Project/repos/Repository/pull-requests/1/comments"))
            .withQueryParam("start", equalTo(String.valueOf(0))).willReturn(
                    aJsonResponse().withStatus(HttpURLConnection.HTTP_OK).withBody(stashJsonComment1)
    ));

    wireMock.stubFor(get(
            urlPathEqualTo("/rest/api/1.0/projects/Project/repos/Repository/pull-requests/1/comments"))
            .withQueryParam("start", equalTo(String.valueOf(1))).willReturn(
                    aJsonResponse().withStatus(HttpURLConnection.HTTP_OK).withBody(stashJsonComment2)
            ));

    StashCommentReport report = client.getPullRequestComments("Project", "Repository", "1", "path");
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

    wireMock.stubFor(get(anyUrl()).withQueryParam("start", equalTo(String.valueOf(0))).willReturn(
            aJsonResponse().withStatus(HttpURLConnection.HTTP_OK).withBody(stashJsonComment1)));

    wireMock.stubFor(get(anyUrl()).withQueryParam("start", equalTo(String.valueOf(1))).willReturn(
            aJsonResponse().withStatus(HttpURLConnection.HTTP_OK).withBody(stashJsonComment2)));

    StashCommentReport report = client.getPullRequestComments("Project", "Repository", "1", "path");
    assertTrue(report.contains("message1", "path", 5));
    assertFalse(report.contains("message2", "path", 10));
    assertEquals(report.size(), 1);
  }
  
  @Test(expected = StashClientException.class)
  public void testGetPullRequestCommentsWithWrongHTTPResult() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_FORBIDDEN)));
    client.getPullRequestComments("Project", "Repository", "1", "path");
  }
  
  @Test(expected = StashClientException.class)
  public void testGetPullRequestCommentsWithException() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withFixedDelay(errorTimeout)));
    client.getPullRequestComments("Project", "Repository", "1", "path");
  }

  @Test
  public void testGetPullRequestDiffs() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_OK).withBody(DiffReportSample.baseReport)));

    StashDiffReport report = client.getPullRequestDiffs("Project", "Repository", "1");
    assertEquals(report.getDiffs().size(), 4);
  }
  
  @Test(expected = StashClientException.class)
  public void testGetPullRequestDiffsWithWrongHTTPResult() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_FORBIDDEN).withBody(DiffReportSample.baseReport)));
    client.getPullRequestDiffs("Project", "Repository", "1");
  }
  
  @Test(expected = StashClientException.class)
  public void testGetPullRequestDiffsWithException() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withFixedDelay(errorTimeout)));
    client.getPullRequestDiffs("Project", "Repository", "1");
  }
  
  @Test
  public void testPostCommentLineOnPullRequest() throws Exception {
    String stashJsonComment = "{\"id\":1234, \"text\":\"message\", \"anchor\": {\"path\":\"path\", \"line\":5},"
        + "\"author\": {\"id\":1, \"name\":\"SonarQube\", \"slug\":\"sonarqube\", \"email\":\"sq@email.com\"}, \"version\": 0}";
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_CREATED).withBody(stashJsonComment)));

    StashComment comment = client.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");
    assertEquals((long) 1234, comment.getId());
  }
  
  @Test
  public void testPostCommentLineOnPullRequestWithWrongHTTPResult() throws Exception {
    addErrorResponse(any(anyUrl()), HTTP_FORBIDDEN);

    try {
      client.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");
      Assert.fail("Wrong HTTP result should raised StashClientException");
    } catch (StashClientException e) {
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("detailed error"));
      Assert.assertThat(e.getMessage(), CoreMatchers.containsString("seriousException"));
    }
  }
  
  @Test(expected = StashClientException.class)
  public void testPostCommentLineOnPullRequestWithException() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_CREATED).withFixedDelay(errorTimeout)));
    client.postCommentLineOnPullRequest("Project", "Repository", "1", "message", "path", 5, "type");
  }

  @Test
  public void testGetUser() throws Exception {
    String jsonUser = "{\"name\":\"SonarQube\", \"email\":\"sq@email.com\", \"id\":1, \"slug\":\"sonarqube\"}";
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withBody(jsonUser)));

    StashUser user = client.getUser("sonarqube");
    
    assertEquals(user.getId(), 1);
    assertEquals(user.getName(), "SonarQube");
    assertEquals(user.getEmail(), "sq@email.com");
    assertEquals(user.getSlug(), "sonarqube");
    
  }
    
  @Test(expected = StashClientException.class)
  public void testGetUserWithWrongHTTPResult() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_FORBIDDEN)));
    client.getUser("sonarqube");
  }

  @Test
  public void testDeletePullRequestComment() throws Exception {
    StashComment stashComment = new StashComment(1234, "message", "path", 42L, testUser, 0);
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_NO_CONTENT)));
    client.deletePullRequestComment("Project", "Repository", "1", stashComment);
    wireMock.verify(deleteRequestedFor(anyUrl()));
  }
  
  @Test
  public void testGetPullRequest() throws Exception {
    String jsonPullRequest = "{\"version\": 1, \"title\":\"PR-Test\", \"description\":\"PR-test\", \"reviewers\": []}";
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withBody(jsonPullRequest)));

    StashPullRequest pullRequest = client.getPullRequest("Project", "Repository", "123");
    
    assertEquals(pullRequest.getId(), "123");
    assertEquals(pullRequest.getProject(), "Project");
    assertEquals(pullRequest.getRepository(), "Repository");
    assertEquals(pullRequest.getVersion(), 1);
  }
    

  @Test
  public void testApprovePullRequest() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse()));
    client.approvePullRequest("Project", "Repository", "123");
    wireMock.verify(postRequestedFor(anyUrl()));
  }
    

  @Test
  public void testResetPullRequestApproval() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse()));
    client.resetPullRequestApproval("Project", "Repository", "123");
    wireMock.verify(deleteRequestedFor(anyUrl()));
  }

  @Test
  public void testAddPullRequestReviewer() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse()));

    ArrayList<StashUser> reviewers = new ArrayList<>();
    reviewers.add(testUser);
    
    client.addPullRequestReviewer("Project", "Repository", "123", 1L, reviewers);
    wireMock.verify(putRequestedFor(anyUrl()));
  }
  
  @Test
  public void testAddPullRequestReviewerWithNoReviewer() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse()));
    client.addPullRequestReviewer("Project", "Repository", "123", 1L, new ArrayList<StashUser>());
    wireMock.verify(putRequestedFor(anyUrl()));
  }
    
  @Test
  public void testPostTaskOnComment() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_CREATED)));
    client.postTaskOnComment("message", 1111L);
    wireMock.verify(postRequestedFor(anyUrl()));
  }

  @Test
  public void testDeleteTaskOnComment() throws Exception {
    wireMock.stubFor(any(anyUrl()).willReturn(aJsonResponse().withStatus(HTTP_NO_CONTENT)));
    StashTask task = new StashTask(1111L, "some text", "some state", true);
    client.deleteTaskOnComment(task);
    wireMock.verify(deleteRequestedFor(anyUrl()));
  }

  @Test
  public void testFollowInternalRedirection() throws Exception {
    String jsonUser = "{\"name\":\"SonarQube\", \"email\":\"sq@email.com\", \"id\":1, \"slug\":\"sonarqube\"}";
    wireMock.stubFor(get(anyUrl()).atPriority(2).willReturn(
            aJsonResponse().withStatus(HTTP_MOVED_TEMP).withHeader("Location", "/foo")));
    wireMock.stubFor(get(urlPathEqualTo("/foo")).atPriority(1).willReturn(aJsonResponse().withBody(jsonUser)));
    client.getUser("does not matter");
    wireMock.verify(getRequestedFor(urlPathEqualTo("/foo")));
  }

  private void addErrorResponse(MappingBuilder mapping, int statusCode) {
    wireMock.stubFor(mapping.willReturn( aJsonResponse()
            .withStatus(statusCode)
            .withHeader("Content-Type", "application/json")
            .withBody("{\n" +
                    "    \"errors\": [\n" +
                    "        {\n" +
                    "            \"context\": null,\n" +
                    "            \"message\": \"A detailed error message.\",\n" +
                    "            \"exceptionName\": \"seriousException\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}")
    ));
  }

  private static ResponseDefinitionBuilder aJsonResponse() {
      return aResponse().withHeader("Content-Type", "application/json").withBody("{}");
  }

  // The first request to wiremock may be slow.
  // We could increase the timeout on our StashClient but then all the timeout test take longer.
  // So instead we perform a dummy request on each test invocation with a high timeout.
  // We now have many more request than before, but are faster anyways.
  private void primeWireMock() throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + wireMock.port()).openConnection();
    conn.setConnectTimeout(1000);
    conn.setConnectTimeout(1000);
    conn.connect();
    conn.getResponseCode();
    wireMock.resetRequests();
  }
}
