package org.sonar.plugins.stash.end2end;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.picocontainer.DefaultPicoContainer;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.sensor.internal.MockAnalysisMode;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.*;
import org.sonar.plugins.stash.fixtures.*;
import org.sonar.plugins.stash.issue.StashUser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.sonar.plugins.stash.TestUtils.notNull;
import static org.sonar.plugins.stash.TestUtils.primeWireMock;

public abstract class EndToEndTest {
  @RegisterExtension
  public WireMockExtension wireMock = new WireMockExtension(
      DummyStashServer.extend(WireMockConfiguration.options().dynamicPort()), true);

  @BeforeEach
  public void setUp() throws Exception {
    primeWireMock(wireMock);
  }

  private File repoRoot = new File("/invalid/");

  protected RunResults run(String fixtureName, Consumer<MapSettings> settingsCustomizer, Collection<PostJobIssue> issues) throws Exception {
    StashUser user = new StashUser(
        // has to match data in fixture
        1,"some.user", "some.user", "some.user@example.org"
    );
    PullRequestRef pr = PullRequestRef.builder()
        .setProject("a")
        .setRepository("b")
        .setPullRequestId(42).build();
    DummyStashServer sqServer = new DummyStashServer(wireMock);
    sqServer.mockUser(user);
    sqServer.mockPrDiff(pr, readFixture(fixtureName));
    sqServer.noCommentsFor(pr);
    sqServer.expectCommentsUpdateFor(pr);

    MapSettings settings = new MapSettings();
    settings.setProperty(StashPlugin.STASH_NOTIFICATION, true);
    settings.setProperty(StashPlugin.STASH_URL, "http://127.0.0.1:" + wireMock.port());
    settings.setProperty(StashPlugin.STASH_TIMEOUT, 400);
    settings.setProperty(StashPlugin.STASH_LOGIN, user.getName());
    settings.setProperty(StashPlugin.STASH_PROJECT, pr.project());
    settings.setProperty(StashPlugin.STASH_REPOSITORY, pr.repository());
    settings.setProperty(StashPlugin.STASH_PULL_REQUEST_ID, pr.pullRequestId());
    settings.setProperty(StashPlugin.STASH_ISSUE_THRESHOLD, Integer.MAX_VALUE);
    settings.setProperty(StashPlugin.STASH_ISSUE_SEVERITY_THRESHOLD, Severity.MINOR);
    settings.setProperty(StashPlugin.STASH_REPOSITORY_ROOT, repoRoot.toString());
    settings.setProperty(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD, Severity.CRITICAL);
    settingsCustomizer.accept(settings);

    MockAnalysisMode mode = new MockAnalysisMode();
    mode.setPreviewOrIssue(true);

    DefaultPicoContainer container = new DefaultPicoContainer();
    container.addComponent(settings.asConfig());
    container.addComponent(DummyServer.class);
    container.addComponent(new DummyStashProjectBuilder(repoRoot));
    container.addComponent(StashPluginConfiguration.class);
    container.addComponent(StashRequestFacade.class);
    container.addComponent(StashIssueReportingPostJob.class);

    PostJobContext context = new DummyPostJobContext(settings, mode, issues);
    StashIssueReportingPostJob job = notNull(container.getComponent(StashIssueReportingPostJob.class));
    job.executeThrowing(context);

    RunResults results = new RunResults();
    results.comments = sqServer.getCreatedComments();
    return results;
  }

  protected static class RunResults {
    public List<JsonObject> comments;
  }

  private static String readFixture(String name) throws IOException {
    // https://stackoverflow.com/a/6068228
    return Resources.toString(
        Resources.getResource("fixtures/" + name + ".json"),
        StandardCharsets.UTF_8
    );
  }

  protected PostJobIssue issue(String key, String ruleKey, String module, String moduleSubDir, String path, int line) {
    return new DummyPostJobIssue(
        repoRoot.toPath().resolve(moduleSubDir), key, RuleKey.of("squid", ruleKey), module, path, line,"some message", org.sonar.api.batch.rule.Severity.MAJOR);
  }
}