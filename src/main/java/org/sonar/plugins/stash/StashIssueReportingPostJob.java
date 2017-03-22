package org.sonar.plugins.stash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;

import java.util.List;

public class StashIssueReportingPostJob implements PostJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashIssueReportingPostJob.class);
  private static final String STACK_TRACE = "Exception stack trace";
  
  private final ProjectIssues projectIssues;
  private final StashPluginConfiguration config;
  private final StashRequestFacade stashRequestFacade;
  private final InputFileCache inputFileCache;
  private final FileSystem fileSystem;

  public StashIssueReportingPostJob(StashPluginConfiguration stashPluginConfiguration, ProjectIssues projectIssues,
                                    StashRequestFacade stashRequestFacade, InputFileCache inputFileCache, FileSystem fileSystem) {
    this.projectIssues = projectIssues;
    this.config = stashPluginConfiguration;
    this.stashRequestFacade = stashRequestFacade;
    this.inputFileCache = inputFileCache;
    this.fileSystem = fileSystem;
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    if (projectIssues != null && projectIssues.issues() != null) {
      for (org.sonar.api.issue.Issue i : projectIssues.issues()) {
        System.out.println(i.ruleKey() + ": " + i.message());
      }
    }

    for (InputFile inputFile : fileSystem.inputFiles(fileSystem.predicates().all())) {
      inputFileCache.putInputFile(context.getResource(inputFile).getEffectiveKey(), inputFile);
    }

    try {
      boolean notifyStash = config.hasToNotifyStash();
      if (notifyStash) {

        // Stash MANDATORY options
        String stashURL  = stashRequestFacade.getStashURL();
        int stashTimeout = config.getStashTimeout();
          
        StashCredentials stashCredentials = stashRequestFacade.getCredentials();

        try (StashClient stashClient = new StashClient(stashURL, stashCredentials, stashTimeout, config.getSonarQubeVersion())) {

          // Down the rabbit hole...
          updateStashWithSonarInfo(stashClient, stashCredentials, project, context);
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
      List<Issue> issueReport = stashRequestFacade.extractIssueReport(projectIssues);


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

      postInfoAndPRsActions(pr, issueReport, issueThreshold, diffReport, stashClient);

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
  private void postInfoAndPRsActions(PullRequestRef pr, List<Issue> issueReport, int issueThreshold,
                                       StashDiffReport diffReport,
                                       StashClient stashClient) {

    // Some local definitions
    boolean canApprovePullrequest = config.canApprovePullRequest();

    int issueTotal     = issueReport.size();

    // if threshold exceeded, do not push issue list to Stash
    if (issueTotal >= issueThreshold) {
      LOGGER.warn("Too many issues detected ({}/{}): Issues cannot be displayed in Diff view",
                                                     issueTotal, issueThreshold);
    } else {
      // publish SonarQube issue and code coverage
      stashRequestFacade.postSonarQubeReport(pr, issueReport, diffReport, stashClient);
    }

    if (config.includeAnalysisOverview()) {
      stashRequestFacade.postAnalysisOverview(pr, issueThreshold, issueReport, stashClient);
    }

    // if no new issues and coverage is improved,
    // plugin approves the pull-request
    if (canApprovePullrequest && (issueTotal == 0)){

      stashRequestFacade.approvePullRequest(pr, stashClient);

    } else if (canApprovePullrequest && ( (issueTotal != 0)) ) {

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
