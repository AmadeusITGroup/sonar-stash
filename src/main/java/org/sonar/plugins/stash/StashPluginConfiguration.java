package org.sonar.plugins.stash;

import com.google.common.collect.Sets;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scanner.ScannerSide;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sonar.plugins.stash.StashPlugin.SEVERITY_NONE;

@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class StashPluginConfiguration {

  private Server server;
  private Configuration configuration;

  public StashPluginConfiguration(Configuration configuration, Server server) {
    this.configuration = configuration;
    this.server = server;
  }

  public boolean hasToNotifyStash() {
    return configuration.getBoolean(StashPlugin.STASH_NOTIFICATION).orElse(false);
  }

  public String getStashProject() {
    return configuration.get(StashPlugin.STASH_PROJECT).orElse(null);
  }

  public String getStashRepository() {
    return configuration.get(StashPlugin.STASH_REPOSITORY).orElse(null);
  }

  public Integer getPullRequestId() {
    return configuration.getInt(StashPlugin.STASH_PULL_REQUEST_ID).orElse(null);
  }

  public String getStashURL() {
    return configuration.get(StashPlugin.STASH_URL).orElse(null);
  }

  public String getStashLogin() {
    return configuration.get(StashPlugin.STASH_LOGIN).orElse(null);
  }

  public String getStashUserSlug() {
    return configuration.get(StashPlugin.STASH_USER_SLUG).orElse(null);
  }

  public String getStashPassword() {
    return configuration.get(StashPlugin.STASH_PASSWORD).orElse(null);
  }

  public String getStashPasswordEnvironmentVariable() {
    return configuration.get(StashPlugin.STASH_PASSWORD_ENVIRONMENT_VARIABLE).orElse(null);
  }

  public String getSonarQubeURL() {
    return server.getPublicRootUrl();
  }

  public String getSonarQubeLogin() {
    return configuration.get(CoreProperties.LOGIN).orElse(null);
  }

  public String getSonarQubePassword() {
    return configuration.get(CoreProperties.PASSWORD).orElse(null);
  }

  public int getIssueThreshold() {
    return configuration
        .getInt(StashPlugin.STASH_ISSUE_THRESHOLD)
        .orElse(StashPlugin.DEFAULT_STASH_THRESHOLD_VALUE);
  }

  public Severity getIssueSeverityThreshold() {
    return Severity.valueOf(
        Objects.requireNonNull(
            configuration.get(StashPlugin.STASH_ISSUE_SEVERITY_THRESHOLD).orElse(SEVERITY_NONE)));
  }

  public int getStashTimeout() {
    return configuration.getInt(StashPlugin.STASH_TIMEOUT).orElse(StashPlugin.DEFAULT_STASH_TIMEOUT_VALUE);
  }

  public boolean canApprovePullRequest() {
    return configuration.getBoolean(StashPlugin.STASH_REVIEWER_APPROVAL).orElse(false);
  }

  public boolean resetComments() {
    return configuration.getBoolean(StashPlugin.STASH_RESET_COMMENTS).orElse(false);
  }

  public Optional<Severity> getTaskIssueSeverityThreshold() {
    return getOptionalSeveritySetting(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD);
  }

  public Optional<Severity> getApprovalSeverityThreshold() {
    return getOptionalSeveritySetting(StashPlugin.STASH_REVIEWER_APPROVAL_SEVERITY_THRESHOLD);
  }

  public boolean includeAnalysisOverview() {
    return configuration.getBoolean(StashPlugin.STASH_INCLUDE_ANALYSIS_OVERVIEW).orElse(StashPlugin.DEFAULT_STASH_ANALYSIS_OVERVIEW);
  }

  public Optional<File> getRepositoryRoot() {
    return Optional.ofNullable(configuration.get(StashPlugin.STASH_REPOSITORY_ROOT).orElse(null)).map(File::new);
  }

  public boolean includeExistingIssues() {
    return configuration.getBoolean(StashPlugin.STASH_INCLUDE_EXISTING_ISSUES).orElse(StashPlugin.DEFAULT_STASH_INCLUDE_EXISTING_ISSUES);
  }

  public int getFilesLimitInOverview() {
    return configuration.getInt(StashPlugin.STASH_FILES_LIMIT_IN_OVERVIEW).orElse(StashPlugin.DEFAULT_STASH_FILES_IN_OVERVIEW);
  }

  public int issueVicinityRange() {
    return configuration.getInt(StashPlugin.STASH_INCLUDE_VICINITY_RANGE).orElse(StashPlugin.DEFAULT_STASH_INCLUDE_VICINITY_RANGE);
  }

  public Set<RuleKey> excludedRules() {
    return Sets.newHashSet(configuration.getStringArray(StashPlugin.STASH_EXCLUDE_RULES)).stream()
        .map(String::trim)
        .map(RuleKey::parse)
        .collect(Collectors.toSet());
  }

  private Optional<Severity> getOptionalSeveritySetting(String key) {
    String setting = configuration.get(key).orElse(SEVERITY_NONE);
    if (SEVERITY_NONE.equals(setting)) {
      return Optional.empty();
    }
    if (setting == null) {
      return Optional.empty();
    }
    return Optional.of(Severity.valueOf(setting));
  }
}
