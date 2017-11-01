package org.sonar.plugins.stash;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Settings;

import java.io.File;
import java.util.Optional;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;

@BatchSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class StashPluginConfiguration {

  private Settings settings;
  private Server server;

  public StashPluginConfiguration(Settings settings, Server server) {
    this.settings = settings;
    this.server = server;
  }

  public boolean hasToNotifyStash() {
    return settings.getBoolean(StashPlugin.STASH_NOTIFICATION);
  }

  public String getStashProject() {
    return settings.getString(StashPlugin.STASH_PROJECT);
  }

  public String getStashRepository() {
    return settings.getString(StashPlugin.STASH_REPOSITORY);
  }

  public Integer getPullRequestId() {
    return settings.getInt(StashPlugin.STASH_PULL_REQUEST_ID);
  }

  public String getStashURL() {
    return settings.getString(StashPlugin.STASH_URL);
  }

  public String getStashLogin() {
    return settings.getString(StashPlugin.STASH_LOGIN);
  }

  public String getStashUserSlug() {
    return settings.getString(StashPlugin.STASH_USER_SLUG);
  }

  public String getStashPassword() {
    return settings.getString(StashPlugin.STASH_PASSWORD);
  }

  public String getStashPasswordEnvironmentVariable() {
    return settings.getString(StashPlugin.STASH_PASSWORD_ENVIRONMENT_VARIABLE);
  }

  public String getSonarQubeURL() {
    return server.getURL();
  }

  public String getSonarQubeLogin() {
    return settings.getString(CoreProperties.LOGIN);
  }

  public String getSonarQubePassword() {
    return settings.getString(CoreProperties.PASSWORD);
  }

  public int getIssueThreshold() {
    return settings.getInt(StashPlugin.STASH_ISSUE_THRESHOLD);
  }

  public int getStashTimeout() {
    return settings.getInt(StashPlugin.STASH_TIMEOUT);
  }

  public boolean canApprovePullRequest() {
    return settings.getBoolean(StashPlugin.STASH_REVIEWER_APPROVAL);
  }

  public boolean resetComments() {
    return settings.getBoolean(StashPlugin.STASH_RESET_COMMENTS);
  }

  public Optional<Severity> getTaskIssueSeverityThreshold() {
    return getOptionalSeveritySetting(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD);
  }

  public Optional<Severity> getApprovalSeverityThreshold() {
    return getOptionalSeveritySetting(StashPlugin.STASH_REVIEWER_APPROVAL_SEVERITY_THRESHOLD);
  }

  public boolean includeAnalysisOverview() {
    return settings.getBoolean(StashPlugin.STASH_INCLUDE_ANALYSIS_OVERVIEW);
  }

  public Optional<File> getRepositoryRoot() {
    return Optional.ofNullable(settings.getString(StashPlugin.STASH_REPOSITORY_ROOT)).map(File::new);
  }

  public boolean includeExistingIssues() {
    return settings.getBoolean(StashPlugin.STASH_INCLUDE_EXISTING_ISSUES);
  }

  public int issueVicinityRange() {
    return settings.getInt(StashPlugin.STASH_INCLUDE_VICINITY_RANGE);
  }

  public Set<RuleKey> excludedRules() {
    return Sets.newHashSet(
        settings.getStringArray(StashPlugin.STASH_EXCLUDE_RULES)
    ).stream()
        .map(String::trim)
        .map(RuleKey::parse)
        .collect(Collectors.toSet());
  }

  private Optional<Severity> getOptionalSeveritySetting(String key) {
    String setting = settings.getString(key);
    if (StashPlugin.SEVERITY_NONE.equals(setting)) {
      return Optional.empty();
    }
    return Optional.of(Severity.valueOf(setting));
  }
}