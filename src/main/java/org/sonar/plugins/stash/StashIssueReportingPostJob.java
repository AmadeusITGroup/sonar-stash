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
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;

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

  @Override
  public void executeOn(Project project, SensorContext context) {
    StashClient stashClient = null;
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
        stashClient = new StashClient(stashURL, stashCredentials, stashTimeout);
        
        StashUser stashUser = stashRequestFacade.getSonarQubeReviewer(stashCredentials.getLogin(), stashClient);
        if (stashUser == null) {
          LOGGER.error("Process stopped: no SonarQube reviewer identified to publish to Stash the SQ analysis"); 
        }
        else {
          
          // Get all changes exposed from Stash differential view of the pull-request
          StashDiffReport diffReport = stashRequestFacade.getPullRequestDiffReport(stashProject, repository, stashPullRequestId, stashClient);
          if (diffReport == null) {
            LOGGER.error("Process stopped: No Stash differential report available to process the SQ analysis"); 
          } else {
          
            // if requested, reset all comments linked to the pull-request
            if (config.resetComments()) {
              stashRequestFacade.resetComments(stashProject, repository, stashPullRequestId, diffReport, stashUser, stashClient);
            }
            
            boolean canApprovePullrequest = config.canApprovePullRequest();
            if (canApprovePullrequest) {
              stashRequestFacade.addPullRequestReviewer(stashProject, repository, stashPullRequestId, stashCredentials.getLogin(), stashClient);
            }
            
            // if threshold exceeded, do not push issue list to Stash
            if (issueReport.countIssues() >= issueThreshold) {
              LOGGER.warn("Too many issues detected ({}/{}): Issues cannot be displayed in Diff view", issueReport.countIssues(), issueThreshold);
            } else {
              stashRequestFacade.postCommentPerIssue(stashProject, repository, stashPullRequestId, sonarQubeURL, issueReport, diffReport, stashClient);
            }

            stashRequestFacade.postAnalysisOverview(stashProject, repository, stashPullRequestId, sonarQubeURL, issueThreshold, issueReport, stashClient);
           
            if (canApprovePullrequest) {
           
              // if no new issues, plugin approves the pull-request 
              if (issueReport.countIssues() == 0) {
                stashRequestFacade.approvePullRequest(stashProject, repository, stashPullRequestId, stashCredentials.getLogin(), stashClient);
              } else {
                stashRequestFacade.resetPullRequestApproval(stashProject, repository, stashPullRequestId, stashCredentials.getLogin(), stashClient);
              }
            }
          }
        }
      }
    } catch (StashConfigurationException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug("Exception stack trace", e);
    } finally {
      if (stashClient != null) {
        stashClient.close();
      }
    }
  }
}
