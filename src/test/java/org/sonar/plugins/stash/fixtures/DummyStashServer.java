package org.sonar.plugins.stash.fixtures;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.issue.StashUser;

public class DummyStashServer {
  private final WireMockServer wireMock;
  private final Map<String, StashUser> users = new HashMap<>();
  private final WireMockResponseCallback responseCallback = new WireMockResponseCallback();
  private final List<JsonObject> createdComments = new ArrayList<>();

  private int commentId = 0;

  public DummyStashServer(WireMockServer wireMock) {
    this.wireMock = wireMock;
  }

  public void mockUser(StashUser user) {
    users.put(user.getSlug(), user);
    mockEntity(user, "users", user.getSlug());
  }

  public void mockPrDiff(PullRequestRef pr, String x) {
    mockResponse(x, "projects", pr.project(), "repos", pr.repository(), "pull-requests", String.valueOf(pr.pullRequestId()), "diff");
  }

  public void noCommentsFor(PullRequestRef pr) {
    mockResponse("{\"comments\": []}", "projects", pr.project(), "repos", pr.repository(), "pull-requests", String.valueOf(pr.pullRequestId()), "comments");
  }

  public void expectCommentsUpdateFor(PullRequestRef pr) {
    wireMock.stubFor(
        post(urlPathEqualTo(
            coreApi( "projects", pr.project(), "repos", pr.repository(), "pull-requests", String.valueOf(pr.pullRequestId()), "comments")))
            .willReturn(responseCallback.callback(aJsonResponse().withStatus(201), (request, response, fileSource, parameters) -> {
                  Gson gson = new Gson();
                  JsonObject d = gson.fromJson(request.getBodyAsString(), JsonObject.class);
                  Map<String, Object> result = new HashMap<>();
                  result.put("id", commentId++);
                  result.put("text", d.get("text").getAsString());
                  result.put("version", 1);
                  result.put("author", getUserFromRequest(request));
                  createdComments.add(d);
                  return Response.Builder.like(response)
                      .but()
                      .body(gson.toJson(result))
                      .build();
                })
            ));
  }

  private StashUser getUserFromRequest(Request request) {
    assertEquals(1, users.size());
    return users.values().iterator().next();
  }

  private void mockResponse(String response, String... urlParts) {
    wireMock.stubFor(
        get(urlPathEqualTo(
            coreApi(urlParts)))
            .willReturn(aJsonResponse().withBody(response)
            ));
  }

  private void mockEntity(Object entity, String... urlParts) {
    wireMock.stubFor(
        get(urlPathEqualTo(
            coreApi(urlParts)))
            .willReturn(aJsonResponse(entity)
            ));
  }

  public List<JsonObject> getCreatedComments() {
    return Collections.unmodifiableList(createdComments);
  }

  private static String urlPath(String... parts) {
    return urlPath(true, parts);
  }

  private static String coreApi(String... parts) {
    List<String> apiParts = Arrays.asList("rest", "api", "1.0");
    ArrayList<String> args = new ArrayList<>(Arrays.asList(parts));
    args.addAll(0, apiParts);
    return urlPath(args.toArray(new String[] {}));
  }

  private static String urlPath(boolean leading, String... parts) {
    String prefix = "";
    if (leading) {
      prefix = "/";
    }
    return prefix + Joiner.on('/').join(parts);
  }

  public static ResponseDefinitionBuilder aJsonResponse() {
    return aResponse().withHeader("Content-Type", "application/json").withBody("{}");
  }

  public static ResponseDefinitionBuilder aJsonResponse(Object entity) {
    Gson gson = new Gson();
    String body = gson.toJson(entity);
    return aJsonResponse().withBody(body);
  }

  public static WireMockConfiguration extend(WireMockConfiguration options) {
    return options.extensions(WireMockResponseCallback.getExtension());
  }
}
