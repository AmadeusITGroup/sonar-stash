package org.sonar.plugins.stash;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.GitBranchNotFoundOrNotUniqueException;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.exceptions.StashFailBuildException;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
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
    
      if (!config.hasToNotifyStash()) {
          return;
      }
      
      SonarQubeIssuesReport issueReport = stashRequestFacade.extractIssueReport(projectIssues, inputFileCache);
      
      try {

          int issueThreshold = stashRequestFacade.getIssueThreshold();
          String sonarQubeURL = config.getSonarQubeURL();

          // Stash MANDATORY options
          String stashURL = stashRequestFacade.getStashURL();
          String stashProject = stashRequestFacade.getStashProject();
          String repository = stashRequestFacade.getStashRepository();
          String stashPullRequestId = stashRequestFacade.getStashPullRequestId();

          int stashTimeout = config.getStashTimeout();

          StashCredentials stashCredentials = stashRequestFacade.getCredentials();

          try (StashClient stashClient = new StashClient(stashURL, stashCredentials, stashTimeout, config.getSonarQubeVersion())) {

              StashUser stashUser = stashRequestFacade.getSonarQubeReviewer(stashCredentials.getLogin(), stashClient);

              if (stashUser == null) {
                  throw new StashMissingElementException("no SonarQube reviewer identified"
                          + " to publish to Stash the SQ analysis");
              }

              // Get all changes exposed from Stash differential view of the pull-request
              StashDiffReport diffReport = stashRequestFacade.getPullRequestDiffReport(stashProject, repository,
                      stashPullRequestId, stashClient);
              if (diffReport == null) {
                  throw new StashMissingElementException("No Stash differential report"
                          + " available to process the SQ analysis");
              }

              // if requested, reset all comments linked to the pull-request
              if (config.resetComments()) {
                  stashRequestFacade.resetComments(stashProject, repository, stashPullRequestId,
                          diffReport, stashUser, stashClient);
              }

              boolean canApprovePullrequest = config.canApprovePullRequest();
              if (canApprovePullrequest) {
                  stashRequestFacade.addPullRequestReviewer(stashProject, repository, stashPullRequestId,
                          stashCredentials.getLogin(), stashClient);
              }

              // if threshold exceeded, do not push issue list to Stash
              if (issueReport.countIssues() >= issueThreshold) {
                  LOGGER.warn("Too many issues detected ({}/{}): Issues cannot be displayed in Diff view",
                          issueReport.countIssues(), issueThreshold);
              } else {
                  stashRequestFacade.postCommentPerIssue(stashProject, repository, stashPullRequestId, sonarQubeURL,
                          issueReport, diffReport, stashClient);
              }

              boolean includeAnalysisOverview = config.includeAnalysisOverview();
              if (includeAnalysisOverview) {
                stashRequestFacade.postAnalysisOverview(stashProject, repository, stashPullRequestId, sonarQubeURL,
                    issueThreshold, issueReport, stashClient);
              }

              // if no new issues, plugin approves the pull-request
              if (canApprovePullrequest && issueReport.countIssues() == 0) {
                  stashRequestFacade.approvePullRequest(stashProject, repository, stashPullRequestId,
                          stashCredentials.getLogin(), stashClient);

              } else if (canApprovePullrequest && issueReport.countIssues() != 0) {
                  stashRequestFacade.resetPullRequestApproval(stashProject, repository, stashPullRequestId,
                          stashCredentials.getLogin(), stashClient);
              }
              
          }


      } catch (GitBranchNotFoundOrNotUniqueException e) {
          LOGGER.info("No unique PR: "+ e.getMessage());
      } catch (StashConfigurationException e) {
          LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
          LOGGER.debug("Exception stack trace", e);

      } catch (StashMissingElementException e) {
          LOGGER.error("Process stopped: {}", e.getMessage());
          LOGGER.debug("Exception stack trace", e);
      } catch (Exception e) {
        LOGGER.error("Something unexpected went wrong: {}", e.getMessage(), e);
      }

      failBuildIfNecessary(project, issueReport);              

  }

  private void failBuildIfNecessary(Project project, SonarQubeIssuesReport issueReport) {
    // also, if there are issues plugin can fail the build
    String failForIssuesWithSeverity = config.failForIssuesWithSeverity();
    LOGGER.debug("issueReport={}",issueReport);
    
    if (issueReport!=null 
        && issueReport.countIssues() > 0
        && StringUtils.isNotBlank(failForIssuesWithSeverity) 
        && !StringUtils.equals(failForIssuesWithSeverity, StashPlugin.SEVERITY_NONE)) {
      List<SonarQubeIssue> issues = issueReport.getIssues();

      int failForIssueSeverityAsInt = Severity.ALL.indexOf(failForIssuesWithSeverity.trim().toUpperCase());

      if (failForIssueSeverityAsInt > -1) {
        int issueCountToFailFor = 0;

        for (SonarQubeIssue issue : issues) {
          int issueSeverityAsInt = Severity.ALL.indexOf(String.valueOf(issue.getSeverity()).trim().toUpperCase());
          if (issueSeverityAsInt >= failForIssueSeverityAsInt) {
            LOGGER.debug("Breaking build because of issue {} that has a severity equal or higher than '{}'",
                issue, failForIssuesWithSeverity);
            issueCountToFailFor++;
          }
        }

        if (issueCountToFailFor > 0) {
          String msg = "Project " + project.getName() + " has " + issueCountToFailFor
              + " issues that are of severity equal or higher than " + failForIssuesWithSeverity;
          LOGGER.error(msg);
          throw new StashFailBuildException(msg);
        }
      } else {
        LOGGER.warn("Invalid configuration for {}: '{}' is not a valid Sonar severity, skipping fail-build-check.",
            StashPlugin.STASH_FAIL_FOR_ISSUES_WITH_SEVERITY, failForIssuesWithSeverity);
      }
      
    }
  }

  /*
     Custom exception to keep nested if statements under control
  */
  private class StashMissingElementException extends Exception {

    public StashMissingElementException(String exc) {
        super(exc);
    }
  }
}
