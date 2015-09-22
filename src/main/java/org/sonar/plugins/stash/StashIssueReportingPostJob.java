package org.sonar.plugins.stash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;

public class StashIssueReportingPostJob implements PostJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashIssueReportingPostJob.class);
  
  private final ProjectIssues projectIssues;
  private final StashPluginConfiguration config;
  private final InputFileCache inputFileCache;
  private final StashRequestFacade stashRequestFacade;
    
  public StashIssueReportingPostJob(StashPluginConfiguration stashPluginConfiguration, ProjectIssues projectIssues,
      InputFileCache inputFileCache, StashRequestFacade stashRequestFacade) {
    this.projectIssues = projectIssues;
    this.config = stashPluginConfiguration;
    this.inputFileCache = inputFileCache;
    this.stashRequestFacade = stashRequestFacade;
  }

  public void executeOn(Project project, SensorContext context) {
    try {
      boolean notifyStash = config.hasToNotifyStash();
      if (notifyStash) {
        
        SonarQubeIssuesReport issueReport = stashRequestFacade.extractIssueReport(projectIssues, inputFileCache);
          
        int issueThreshold = stashRequestFacade.getIssueThreshold();
        String sonarQubeURL = config.getSonarQubeURL();
          
        // Stash MANDATORY options
        String stashURL = stashRequestFacade.getStashURL();
        String stashProject = stashRequestFacade.getStashProject();
        String repository = stashRequestFacade.getStashRepository();
        String stashPullRequestId = stashRequestFacade.getStashPullRequestId();
          
        int stashTimeout = config.getStashTimeout();
          
        StashCredentials stashCredentials = stashRequestFacade.getCredentials();
        StashClient stashClient = new StashClient(stashURL, stashCredentials, stashTimeout);
          
        // if threshold exceeded, do not push issue list to Stash
        if (issueReport.countIssues() >= issueThreshold) {
          LOGGER.warn("Too many issues detected ({}/{}): Issues cannot be displayed in Diff view", issueReport.countIssues(), issueThreshold);
        } else {
          stashRequestFacade.postCommentPerIssue(stashProject, repository, stashPullRequestId, sonarQubeURL, issueReport, stashClient);
        }

        stashRequestFacade.postAnalysisOverview(stashProject, repository, stashPullRequestId, sonarQubeURL, issueThreshold, issueReport, stashClient);

      }
    } catch (StashConfigurationException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug("Exception stack trace", e);
    }
  }
}
