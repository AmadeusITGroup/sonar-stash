package org.sonar.plugins.stash;

import java.util.Optional;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;

import static org.junit.Assert.assertEquals;

public class StashPluginConfigurationTest {


  @Test
  public void testStashPluginConfiguration_ConstructorAndAccessors() {

    Integer SPRI = 1337;

    Settings settings = new Settings();
    settings.setProperty(StashPlugin.STASH_NOTIFICATION, true);
    settings.setProperty(StashPlugin.STASH_PROJECT, "take-over-the-world");
    settings.setProperty(StashPlugin.STASH_REPOSITORY, "death-ray");
    settings.setProperty(StashPlugin.STASH_PULL_REQUEST_ID, SPRI);
    settings.setProperty(StashPlugin.STASH_URL, "https://stash");
    settings.setProperty(StashPlugin.STASH_LOGIN, "me");
    settings.setProperty(StashPlugin.STASH_USER_SLUG, "mini.me");
    settings.setProperty(StashPlugin.STASH_PASSWORD, "unsecure");
    settings.setProperty(StashPlugin.STASH_PASSWORD_ENVIRONMENT_VARIABLE, "you-should-not");

    settings.setProperty(CoreProperties.LOGIN, "him");
    settings.setProperty(CoreProperties.PASSWORD, "notsafe");
    settings.setProperty(StashPlugin.STASH_ISSUE_THRESHOLD, 42000);
    settings.setProperty(StashPlugin.STASH_TIMEOUT, 42);
    settings.setProperty(StashPlugin.STASH_REVIEWER_APPROVAL, true);
    settings.setProperty(StashPlugin.STASH_RESET_COMMENTS, false);
    settings.setProperty(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD, "MINOR");
    settings.setProperty(StashPlugin.STASH_INCLUDE_ANALYSIS_OVERVIEW, true);
    //Optional getRepositoryRoot() ???

    StashPluginConfiguration SPC = new StashPluginConfiguration(settings, null);

    assertEquals(true, SPC.hasToNotifyStash());
    assertEquals("take-over-the-world", SPC.getStashProject());
    assertEquals("death-ray", SPC.getStashRepository());
    assertEquals(SPRI, SPC.getPullRequestId());
    assertEquals("https://stash", SPC.getStashURL());
    assertEquals("me", SPC.getStashLogin());
    assertEquals("mini.me", SPC.getStashUserSlug());
    assertEquals("unsecure", SPC.getStashPassword());
    assertEquals("you-should-not", SPC.getStashPasswordEnvironmentVariable());

    assertEquals("him", SPC.getSonarQubeLogin());
    assertEquals("notsafe", SPC.getSonarQubePassword());
    assertEquals(42000, SPC.getIssueThreshold());
    assertEquals(42, SPC.getStashTimeout());
    assertEquals(true, SPC.canApprovePullRequest());
    assertEquals(false, SPC.resetComments());
    assertEquals(Optional.of(Severity.MINOR), SPC.getTaskIssueSeverityThreshold());
    assertEquals(true, SPC.includeAnalysisOverview());
    //assertEquals(, SPC.getRepositoryRoot());

    settings.setProperty(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD, "NONE");
    assertEquals(Optional.empty(), SPC.getTaskIssueSeverityThreshold());
  }

}