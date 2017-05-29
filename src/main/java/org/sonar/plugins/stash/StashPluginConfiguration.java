package org.sonar.plugins.stash;

import java.util.Optional;

import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;

import java.io.File;

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

  public Integer getPullRequestId() {
    return settings.getInt(StashPlugin.STASH_PULL_REQUEST_ID);
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

  public String getStashPasswordEnvironmentVariable() {
    return settings.getString(StashPlugin.STASH_PASSWORD_ENVIRONMENT_VARIABLE);
  }

  public String getSonarQubeURL() {
    return settings.getString(StashPlugin.SONARQUBE_URL);
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

  public String getTaskIssueSeverityThreshold() {
    return settings.getString(StashPlugin.STASH_TASK_SEVERITY_THRESHOLD);
  }

  public String getSonarQubeVersion() {
    return settings.getString(CoreProperties.SERVER_VERSION);
  }

  public boolean includeAnalysisOverview() {
    return settings.getBoolean(StashPlugin.STASH_INCLUDE_ANALYSIS_OVERVIEW);
  }

  public Optional<File> getRepositoryRoot() {
    return Optional.ofNullable(settings.getString(StashPlugin.STASH_REPOSITORY_ROOT)).map(File::new);
  }

  /**
   * Sonar property -Dsonar.branch
   */
  public String getSonarBranch() {
    return settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
  }


  /**
   * This plugin specific property -Dsonar.stash.branch
   */
  public String getSonarStashBranch() {
    return settings.getString(StashPlugin.STASH_BRANCH_NAME);
  }
}