package org.sonar.plugins.stash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.plugins.stash.TestUtils.primeWireMock;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.plugins.stash.fixtures.DummyStashServer;
import org.sonar.plugins.stash.fixtures.MavenSonarFixtures;
import org.sonar.plugins.stash.fixtures.SonarQubeRule;
import org.sonar.plugins.stash.fixtures.SonarScanner;
import org.sonar.plugins.stash.fixtures.WireMockExtension;
import org.sonar.plugins.stash.issue.StashUser;
import org.sonar.plugins.stash.issue.collector.DiffReportSample;

public class CompleteITCase {
  protected static SonarScanner sonarScanner;
  protected File sourcesDir;

  protected static final StashUser user = new StashUser(
      // has to match data in fixture
      1,"sonarqube", "sonarqube", "sq@example.com"
  );
  protected static final String stashPassword = "myPassword";
  protected static final String stashProject = "PROJ";
  protected static final String stashRepo = "REPO";
  protected String projectKey;
  protected String projectName;
  protected static final int stashPullRequest = 42;
  protected DummyStashServer stash;
  protected static final PullRequestRef pr = new PullRequestRef.Builder()
      .setProject(stashProject)
      .setRepository(stashRepo)
      .setPullRequestId(stashPullRequest)
      .build();

  @RegisterExtension
  public WireMockExtension wireMock = new WireMockExtension(
      DummyStashServer.extend(WireMockConfiguration.options().dynamicPort()), true);

  @RegisterExtension
  public static SonarQubeRule sonarqube = new SonarQubeRule(MavenSonarFixtures.getSonarqube(9000));

  @BeforeAll
  public static void setUpClass() throws Exception {
    sonarqube.get().installPlugin(new File(System.getProperty("test.plugin.archive")));
    sonarqube.start();

    sonarScanner = MavenSonarFixtures.getSonarScanner();

  }

  @BeforeEach
  public void setUp(TestInfo testInfo) throws Exception {
    sourcesDir = Paths.get(
        System.getProperty("test.sources.dir"),
        testInfo.getTestMethod().get().getName()
    ).toFile();
    projectKey = testInfo.getTestMethod().get().getName();
    projectName = testInfo.getDisplayName();

    sourcesDir.mkdirs();
    sonarqube.get().createProject(projectKey, projectName);

    primeWireMock(wireMock);
    stash = new DummyStashServer(wireMock);
    stash.mockUser(user);
  }

  @Test
  public void testBasic() throws Exception {
    stash.mockPrDiff(pr, DiffReportSample.baseReport);
    stash.noCommentsFor(pr);
    stash.expectCommentsUpdateFor(pr);

    Properties props = new Properties();
    props.put("sonar.sources", ".");
    scan(true, true, props);
    wireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching(".*" + user.getSlug() + "$")));
    wireMock.verify(WireMock.getRequestedFor(WireMock.urlPathMatching(".*diff$")));
    wireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching(".*comments$")));

    // Making sure we find the proper agent info in a string like: User-Agent: SonarQube/4.5.7 Stash/1.2.0 AHC/1.0
    wireMock.verify(WireMock.getRequestedFor(WireMock.anyUrl())
                            .withHeader("User-Agent", WireMock.matching("^(.*)Stash/[0-9.]+(.*)$")));
    wireMock.verify(WireMock.getRequestedFor(WireMock.anyUrl())
                            .withHeader("User-Agent", WireMock.matching("^(.*)SonarQube/[0-9.]+(.*)$")));
  }

  @Test
  public void testMultiModule() throws Exception {
    installFile("sonar-project.properties");
    targetLocation("module1/src/main/java").toFile().mkdirs();
    targetLocation("module2/src/main/java").toFile().mkdirs();

    scan(false, false, null);

    installFile("module1/src/main/java/Foo.java");
    installFile("module2/src/main/java/Bar.java");


    stash.mockPrDiff(
        pr,
        "{\"fromHash\":\"bf19fb766666d80486aa81bc728a4e394ffa7ea8\",\"toHash\":\"79748088d6810b6e29eaa0319228fb8fecce14bb\",\"contextLines\":10,\"whitespace\":\"SHOW\",\"diffs\":[{\"source\":null,\"destination\":{\"components\":[\"module1\",\"src\",\"main\",\"java\",\"Foo.java\"],\"parent\":\"module1/src/main/java\",\"name\":\"Foo.java\",\"extension\":\"java\",\"toString\":\"module1/src/main/java/Foo.java\"},\"hunks\":[{\"sourceLine\":0,\"sourceSpan\":0,\"destinationLine\":1,\"destinationSpan\":5,\"segments\":[{\"type\":\"ADDED\",\"lines\":[{\"source\":0,\"destination\":1,\"line\":\"public class Foo {\",\"truncated\":false},{\"source\":0,\"destination\":2,\"line\":\"    public static void main(String[] args) {\",\"truncated\":false},{\"source\":0,\"destination\":3,\"line\":\"        System.out.println(\\\"Foo\\\");\",\"truncated\":false},{\"source\":0,\"destination\":4,\"line\":\"    }\",\"truncated\":false},{\"source\":0,\"destination\":5,\"line\":\"}\",\"truncated\":false}],\"truncated\":false}],\"truncated\":false}],\"truncated\":false},{\"source\":null,\"destination\":{\"components\":[\"module2\",\"src\",\"main\",\"java\",\"Bar.java\"],\"parent\":\"module2/src/main/java\",\"name\":\"Bar.java\",\"extension\":\"java\",\"toString\":\"module2/src/main/java/Bar.java\"},\"hunks\":[{\"sourceLine\":0,\"sourceSpan\":0,\"destinationLine\":1,\"destinationSpan\":5,\"segments\":[{\"type\":\"ADDED\",\"lines\":[{\"source\":0,\"destination\":1,\"line\":\"public class Bar {\",\"truncated\":false},{\"source\":0,\"destination\":2,\"line\":\"    public static void main(String[] args) {\",\"truncated\":false},{\"source\":0,\"destination\":3,\"line\":\"        System.out.println(\\\"Bar\\\");\",\"truncated\":false},{\"source\":0,\"destination\":4,\"line\":\"    }\",\"truncated\":false},{\"source\":0,\"destination\":5,\"line\":\"}\",\"truncated\":false}],\"truncated\":false}],\"truncated\":false}],\"truncated\":false}],\"truncated\":false}"
    );
    stash.noCommentsFor(pr);
    stash.expectCommentsUpdateFor(pr);

    scan(true, true, null);

    List<JsonObject> comments = stash.getCreatedComments();
    assertThat(comments).hasSize(5);

    List<JsonObject> overviewComment = comments.stream().filter(c -> !c.has("anchor")).collect(Collectors.toList());
    assertThat(overviewComment).hasSize(1);
    assertThat(overviewComment.get(0).get("text").getAsString()).contains("SonarQube analysis Overview");

    List<JsonObject> fileComments = comments.stream().filter(c -> c.has("anchor")).collect(Collectors.toList());
    assertThat(fileComments).hasSize(4);
    assertThat(fileComments).extracting(o -> o.get("anchor").getAsJsonObject().get("path").getAsString()).containsOnly(
        "module1/src/main/java/Foo.java", "module2/src/main/java/Bar.java"
    );
  }

  private Path targetLocation(String name) {
    return sourcesDir.toPath().resolve(name);
  }

  private void installFile(String name) throws IOException {
    Path target = targetLocation(name);
    target.getParent().toFile().mkdirs();
    Files.copy(
        getClass().getClassLoader().getResourceAsStream("foo/" + name),
        target
    );
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

  protected void scan(boolean activateSonarStash, boolean issuesMode, Properties properties) throws Exception {
    List<File> sources = new ArrayList<>();
    sources.add(sourcesDir);
    Properties extraProps = new Properties();
    //extraProps.setProperty("sonar.analysis.mode", "incremental");
    if (activateSonarStash) {
      extraProps.setProperty("sonar.stash.url", "http://127.0.0.1:" + wireMock.port());
      extraProps.setProperty("sonar.stash.login", user.getSlug());
      extraProps.setProperty("sonar.stash.password", stashPassword);
      extraProps.setProperty("sonar.stash.notification", "true");
      extraProps.setProperty("sonar.stash.project", stashProject);
      extraProps.setProperty("sonar.stash.repository", stashRepo);
      extraProps.setProperty("sonar.stash.pullrequest.id", String.valueOf(stashPullRequest));
    }
    if (issuesMode) {
      extraProps.put("sonar.analysis.mode", "issues");
    }
    extraProps.setProperty("sonar.log.level", "DEBUG");

    if (properties != null) {
      extraProps.putAll(properties);
    }
    sonarScanner.scan(sonarqube.get(), sourcesDir, projectKey, projectName, "0.0.0Final39", extraProps);
  }
}
