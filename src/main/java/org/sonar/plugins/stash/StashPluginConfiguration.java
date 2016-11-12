package org.sonar.plugins.stash;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;

import java.util.Arrays;
import java.util.List;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class StashPluginConfiguration implements BatchComponent {

  private Settings settings;

  public StashPluginConfiguration(Settings settings) {
    this.settings = settings;
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

  public String getPullRequestId() {
    return settings.getString(StashPlugin.STASH_PULL_REQUEST_ID);
  }

  public String getStashURL() {
    return settings.getString(StashPlugin.STASH_URL);
  }

  public String getStashLogin() {
    return settings.getString(StashPlugin.STASH_LOGIN);
  }

  public String getStashPassword() {
    return settings.getString(StashPlugin.STASH_PASSWORD);
  }

  public String getSonarQubeURL() {
    return settings.getString(StashPlugin.SONARQUBE_URL);
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
  
  public String getTaskIssueSeverityThreshold() {
    return settings.getString(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD);
  }

  public boolean includeAnalysisOverview() {
    return settings.getBoolean(StashPlugin.STASH_INCLUDE_ANALYSIS_OVERVIEW);
  }

  public boolean includeExistingIssues() {
    return settings.getBoolean(StashPlugin.STASH_INCLUDE_EXISTING_ISSUES);
  }

  public boolean includeVicinityIssues() {
    return settings.getBoolean(StashPlugin.STASH_INCLUDE_VICINITY_ISSUES);
  }

  public List<String> excludedRules() {
    return Arrays.asList(settings.getStringArray(StashPlugin.STASH_EXCLUDE_RULES));
  }
}