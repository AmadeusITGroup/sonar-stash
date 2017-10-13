package org.sonar.plugins.stash;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.plugins.stash.fixtures.MavenSonarFixtures;
import org.sonar.plugins.stash.fixtures.SonarQubeRule;
import org.sonar.plugins.stash.fixtures.SonarScanner;
import org.sonar.plugins.stash.issue.collector.DiffReportSample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.sonar.plugins.stash.client.StashClientTest.aJsonResponse;

public class CompleteITCase {
  protected static SonarScanner sonarScanner;
  protected static File sourcesDir;

  protected static final String stashUser = "myUser";
  protected static final String stashPassword = "myPassword";
  protected static final String stashProject = "PROJ";
  protected static final String stashRepo = "REPO";
  protected static final String sonarQubeKey = "SomeKey";
  protected static final String sonarQubeName = "Integration test project";
  protected static final int stashPullRequest = 42;

  @Rule
  public WireMockRule wireMock = new WireMockRule(WireMockConfiguration.options().dynamicPort());
  @ClassRule
  public static SonarQubeRule sonarqube = new SonarQubeRule(MavenSonarFixtures.getSonarqube(9000));

  @BeforeClass
  public static void setUpClass() throws Exception {
    sonarqube.get().installPlugin(new File(System.getProperty("test.plugin.archive")));
    sonarqube.start();
    sonarqube.get().createProject(sonarQubeKey, sonarQubeName);

    sonarScanner = MavenSonarFixtures.getSonarScanner();

    sourcesDir = new File(System.getProperty("test.sources.dir"));
    sourcesDir.mkdirs();
  }

  @Test
  public void basicTest() throws Exception {
    String jsonUser = "{\"name\":\"SonarQube\", \"email\":\"sq@email.com\", \"id\":1, \"slug\":\"sonarqube\"}";
    String jsonPullRequest = DiffReportSample.baseReport;
    wireMock.stubFor(
        WireMock.get(WireMock.urlPathEqualTo(
            urlPath("rest", "api", "1.0", "users", stashUser)))
                .willReturn(aJsonResponse()
                                .withBody(jsonUser)
                )
    );
    wireMock.stubFor(
        WireMock.get(WireMock.urlPathEqualTo(
            repoPath(stashProject, stashRepo, "pull-requests", String.valueOf(stashPullRequest), "diff")))
                .withQueryParam("withComments", WireMock.equalTo(String.valueOf(true)))
                .willReturn(aJsonResponse()
                                .withBody(jsonPullRequest)
                )
    );
    wireMock.stubFor(
        WireMock.post(WireMock.urlPathEqualTo(
            repoPath(stashProject, stashRepo, "pull-requests", String.valueOf(stashPullRequest), "comments")))
                .withRequestBody(WireMock.equalToJson(
                    "{\"text\":\"## SonarQube analysis Overview\\n### No new issues detected!\\n\\n\"}"))
                .willReturn(aJsonResponse()
                                .withStatus(201)
                                .withBody("{}")
                )
    );

    scan();
    wireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching(".*" + stashUser + "$")));
    wireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching(".*diff$")));
    wireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching(".*comments$")));

    // Making sure we find the proper agent info in a string like: User-Agent: SonarQube/4.5.7 Stash/1.2.0 AHC/1.0
    wireMock.verify(WireMock.getRequestedFor(WireMock.anyUrl())
                            .withHeader("User-Agent", WireMock.matching("^(.*)Stash/[0-9.]+(.*)$")));
    wireMock.verify(WireMock.getRequestedFor(WireMock.anyUrl())
                            .withHeader("User-Agent", WireMock.matching("^(.*)SonarQube/[0-9.]+(.*)$")));
  }

  private String repoPath(String project, String repo, String... parts) {
    return urlPath("rest", "api", "1.0", "projects", project, "repos", repo, urlPath(false, parts));
  }

  private String urlPath(String... parts) {
    return urlPath(true, parts);
  }

  private String urlPath(boolean leading, String... parts) {
    String prefix = "";
    if (leading) {
      prefix = "/";
    }
    return prefix + StringUtils.join(parts, '/');
  }

  protected void scan() throws Exception {
    List<File> sources = new ArrayList<>();
    sources.add(sourcesDir);
    Properties extraProps = new Properties();
    //extraProps.setProperty("sonar.analysis.mode", "incremental");
    extraProps.setProperty("sonar.stash.url", "http://127.0.0.1:" + wireMock.port());
    extraProps.setProperty("sonar.stash.login", stashUser);
    extraProps.setProperty("sonar.stash.password", stashPassword);
    extraProps.setProperty("sonar.stash.notification", "true");
    extraProps.setProperty("sonar.stash.project", stashProject);
    extraProps.setProperty("sonar.stash.repository", stashRepo);
    extraProps.setProperty("sonar.stash.pullrequest.id", String.valueOf(stashPullRequest));
    extraProps.setProperty("sonar.log.level", "DEBUG");
    sonarScanner.scan(sonarqube.get(), sourcesDir, sources, sonarQubeKey, sonarQubeName, "0.0.0Final39", extraProps);
  }
}
