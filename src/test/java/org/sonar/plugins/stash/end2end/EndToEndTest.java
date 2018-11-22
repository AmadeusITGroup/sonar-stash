package org.sonar.plugins.stash.end2end;

import static org.sonar.plugins.stash.TestUtils.notNull;
import static org.sonar.plugins.stash.TestUtils.primeWireMock;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.picocontainer.DefaultPicoContainer;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.sensor.internal.MockAnalysisMode;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.DummyStashProjectBuilder;
import org.sonar.plugins.stash.PullRequestRef;
import org.sonar.plugins.stash.StashIssueReportingPostJob;
import org.sonar.plugins.stash.StashPlugin;
import org.sonar.plugins.stash.StashPluginConfiguration;
import org.sonar.plugins.stash.StashRequestFacade;
import org.sonar.plugins.stash.fixtures.DummyPostJobContext;
import org.sonar.plugins.stash.fixtures.DummyPostJobIssue;
import org.sonar.plugins.stash.fixtures.DummyServer;
import org.sonar.plugins.stash.fixtures.DummyStashServer;
import org.sonar.plugins.stash.issue.StashUser;

public class EndToEndTest {
  @Rule
  public WireMockRule wireMock = new WireMockRule(
      DummyStashServer.extend(WireMockConfiguration.options().dynamicPort()), true);

  @Before
  public void setUp() throws Exception {
    primeWireMock(wireMock);
  }

  private File repoRoot = new File("/invalid/");

  protected RunResults run(String fixtureName, Consumer<Settings> settingsCustomizer, Collection<PostJobIssue> issues) throws Exception {
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

    Settings settings = new Settings();
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
    container.addComponent(settings);
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