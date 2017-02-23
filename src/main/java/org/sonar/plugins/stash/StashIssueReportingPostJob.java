package org.sonar.plugins.stash;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.client.SonarQubeClient;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.client.StashServerClient;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.CoverageIssuesReport;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;

public class StashIssueReportingPostJob implements PostJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashIssueReportingPostJob.class);
  private static final String STACK_TRACE = "Exception stack trace";
  
  private final ProjectIssues projectIssues;
  private final StashPluginConfiguration config;
  private final InputFileCache inputFileCache;
  private final StashRequestFacade stashRequestFacade;
  private final InputFileCacheSensor inputFileCacheSensor;
    
  public StashIssueReportingPostJob(StashPluginConfiguration stashPluginConfiguration, ProjectIssues projectIssues,
      InputFileCache inputFileCache, StashRequestFacade stashRequestFacade, InputFileCacheSensor inputFileCacheSensor) {
    this.projectIssues = projectIssues;
    this.config = stashPluginConfiguration;
    this.inputFileCache = inputFileCache;
    this.stashRequestFacade = stashRequestFacade;
    this.inputFileCacheSensor = inputFileCacheSensor;
  }

  @Override
  public void executeOn(Project project, SensorContext context) {

    try {
      boolean notifyStash = config.hasToNotifyStash();
      if (notifyStash) {

        // Stash MANDATORY options
        String stashURL  = stashRequestFacade.getStashURL();
        int stashTimeout = config.getStashTimeout();
          
        StashCredentials stashCredentials = stashRequestFacade.getCredentials();

        try (StashClient stashClient = new StashServerClient(stashURL, stashCredentials, stashTimeout, config.getSonarQubeVersion())) {

          // Down the rabbit hole...
          updateStashWithSonarInfo(stashClient, stashCredentials, project, context);
        } catch (Exception e) {
          // ignored
        }
      }
    } catch (StashConfigurationException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }


  /*
  * Second part of the code necessary for the executeOn() -- squid:S134
  */
  private void updateStashWithSonarInfo(StashClient stashClient, StashCredentials stashCredentials,
                                                Project project, SensorContext context) {

    try {
      int issueThreshold  = stashRequestFacade.getIssueThreshold();
      String sonarQubeURL = config.getSonarQubeURL();

      PullRequestRef pr = stashRequestFacade.getPullRequest();

      // SonarQube objects
      SonarQubeClient sonarqubeClient   = new SonarQubeClient(sonarQubeURL);
      SonarQubeIssuesReport issueReport = stashRequestFacade.extractIssueReport(projectIssues, inputFileCache);


      StashUser stashUser = stashRequestFacade.getSonarQubeReviewer(stashCredentials.getLogin(), stashClient);

      if (stashUser == null) {
        throw new StashMissingElementException("No SonarQube reviewer identified to publish to Stash the SQ analysis");
      }

      // Get all changes exposed from Stash differential view of the pull-request
      StashDiffReport diffReport = stashRequestFacade.getPullRequestDiffReport(pr, stashClient);
      if (diffReport == null) {
        throw new StashMissingElementException("No Stash differential report available to process the SQ analysis");
      }

      // if requested, reset all comments linked to the pull-request
      if (config.resetComments()) {
        stashRequestFacade.resetComments(pr, diffReport, stashUser, stashClient);
      }

      boolean canApprovePullrequest = config.canApprovePullRequest();
      if (canApprovePullrequest) {
        stashRequestFacade.addPullRequestReviewer(pr, stashCredentials.getLogin(), stashClient);
      }

      // build code coverage if requested
      CoverageIssuesReport coverageReport = new CoverageIssuesReport();

      String codeCoverageSeverity = stashRequestFacade.getCodeCoverageSeverity();

      if (! StringUtils.equals(codeCoverageSeverity, StashPlugin.SEVERITY_NONE)) {
        coverageReport = stashRequestFacade.getCoverageReport(project.getKey(), context,
                                                                   inputFileCacheSensor,
                                                                   codeCoverageSeverity, sonarqubeClient);
      }

      postInfoAndPRsActions(pr, issueReport, issueThreshold, diffReport, coverageReport, stashClient, sonarqubeClient);

    } catch (StashConfigurationException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);

    } catch (StashMissingElementException e) {
      LOGGER.error("Process stopped: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }


  /*
  * Second part of the code necessary for the updateStashWithSonarInfo() method
  *   and third part of the executeOn() method (call of a call) -- squid:MethodCyclomaticComplexity
  */
  private void postInfoAndPRsActions(PullRequestRef pr, SonarQubeIssuesReport issueReport, int issueThreshold,
                                       StashDiffReport diffReport, CoverageIssuesReport coverageReport,
                                       StashClient stashClient, SonarQubeClient sonarQubeClient) {

    // Some local definitions
    boolean canApprovePullrequest = config.canApprovePullRequest();

    int issueNumber    = issueReport.countIssues() + coverageReport.countLoweredIssues();
    int issueTotal     = issueReport.countIssues();

    String sonarQubeURL = sonarQubeClient.getBaseUrl();

    // if threshold exceeded, do not push issue list to Stash
    if (issueNumber >= issueThreshold) {
      LOGGER.warn("Too many issues detected ({}/{}): Issues cannot be displayed in Diff view",
                                                     issueTotal, issueThreshold);
    } else {
      // publish SonarQube issue and code coverage
      stashRequestFacade.postSonarQubeReport(pr, sonarQubeURL, issueReport, diffReport, stashClient);
      stashRequestFacade.postCoverageReport(pr, sonarQubeURL, coverageReport, diffReport, stashClient);
    }

    if (config.includeAnalysisOverview()) {
      stashRequestFacade.postAnalysisOverview(pr, sonarQubeURL, issueThreshold, issueReport, coverageReport, stashClient);
    }

    // if no new issues and coverage is improved,
    // plugin approves the pull-request
    if (canApprovePullrequest && (issueNumber == 0) && (coverageReport.getEvolution() >= 0)){

      stashRequestFacade.approvePullRequest(pr, stashClient);

    } else if (canApprovePullrequest && ( (issueNumber != 0) || coverageReport.getEvolution() < 0) ) {

      stashRequestFacade.resetPullRequestApproval(pr, stashClient);
    }
  }


  /*
  *  Custom exception to keep nested if statements under control
  */
  private class StashMissingElementException extends Exception {

    public StashMissingElementException(String exc) {
        super(exc);
    }
  }
}
