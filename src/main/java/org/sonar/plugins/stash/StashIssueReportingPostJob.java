package org.sonar.plugins.stash;

import java.util.Collection;
import java.util.Optional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.platform.Server;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;

public class StashIssueReportingPostJob implements PostJob, BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashIssueReportingPostJob.class);
  private static final String STACK_TRACE = "Exception stack trace";

  private final ProjectIssues projectIssues;
  private final StashPluginConfiguration config;
  private final StashRequestFacade stashRequestFacade;
  private final Server sonarQubeServer;

  public StashIssueReportingPostJob(StashPluginConfiguration stashPluginConfiguration,
      ProjectIssues projectIssues,
      StashRequestFacade stashRequestFacade,
      Server sonarQubeServer) {
    this.projectIssues = projectIssues;
    this.config = stashPluginConfiguration;
    this.stashRequestFacade = stashRequestFacade;
    this.sonarQubeServer = sonarQubeServer;
  }

  @Override
  public void executeOn(Project project, SensorContext context) {
    if (!config.hasToNotifyStash()) {
      LOGGER.info("{} not enabled, skipping", this);
      return;
    }

    try {
      // Stash MANDATORY options
      String stashURL = stashRequestFacade.getStashURL();
      int stashTimeout = config.getStashTimeout();

      StashCredentials stashCredentials = stashRequestFacade.getCredentials();
	  boolean acceptAnyCertificate = config.acceptAnyCertificate();
      try (StashClient stashClient = new StashClient(stashURL,
          stashCredentials,
          stashTimeout,
          sonarQubeServer.getVersion(), acceptAnyCertificate)) {

        // Down the rabbit hole...
        updateStashWithSonarInfo(project, stashClient, stashCredentials);
      }
    } catch (StashConfigurationException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }


  /*
  * Second part of the code necessary for the executeOn() -- squid:S134
  */
  private void updateStashWithSonarInfo(Project project, StashClient stashClient,
      StashCredentials stashCredentials) {

    try {
      int issueThreshold = stashRequestFacade.getIssueThreshold();
      PullRequestRef pr = stashRequestFacade.getPullRequest();

      // SonarQube objects
      List<Issue> issueReport = stashRequestFacade.extractIssueReport(projectIssues, project);

      StashUser stashUser = stashRequestFacade
          .getSonarQubeReviewer(stashCredentials.getUserSlug(), stashClient);

      if (stashUser == null) {
        throw new StashMissingElementException(
            "No SonarQube reviewer identified to publish to Stash the SQ analysis");
      }

      // Get all changes exposed from Stash differential view of the pull-request
      StashDiffReport diffReport = stashRequestFacade.getPullRequestDiffReport(pr, stashClient);
      if (diffReport == null) {
        throw new StashMissingElementException(
            "No Stash differential report available to process the SQ analysis");
      }

      // if requested, reset all comments linked to the pull-request
      if (config.resetComments()) {
        stashRequestFacade.resetComments(pr, diffReport, stashUser, stashClient);
      }

      boolean canApprovePullrequest = config.canApprovePullRequest();
      if (canApprovePullrequest) {
        stashRequestFacade.addPullRequestReviewer(pr, stashCredentials.getUserSlug(), stashClient);
      }

      postInfoAndPRsActions(pr, issueReport, issueThreshold, diffReport, stashClient, project);

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
  private void postInfoAndPRsActions(
      PullRequestRef pr, List<Issue> issueReport, int issueThreshold,
      StashDiffReport diffReport, StashClient stashClient,
      Project project
  ) {

    int issueTotal = issueReport.size();

    // if threshold exceeded, do not push issue list to Stash
    if (issueTotal >= issueThreshold) {
      LOGGER.warn("Too many issues detected ({}/{}): Issues cannot be displayed in Diff view",
          issueTotal, issueThreshold);
    } else {
      stashRequestFacade.postSonarQubeReport(pr, issueReport, diffReport, stashClient);
    }

    if (config.includeAnalysisOverview()) {
      stashRequestFacade.postAnalysisOverview(pr, issueReport, stashClient, project);
    }

    if (config.canApprovePullRequest()) {
      if (shouldApprovePullRequest(config.getApprovalSeverityThreshold(), issueReport)) {
        stashRequestFacade.approvePullRequest(pr, stashClient);
      } else {
        stashRequestFacade.resetPullRequestApproval(pr, stashClient);
      }
    }
  }

  static boolean shouldApprovePullRequest(Optional<String> approvalSeverityThreshold, Collection<Issue> report) {
    if (approvalSeverityThreshold.isPresent()) {
      SeverityComparator sc = new SeverityComparator();
      return report.stream().noneMatch(issue ->
          sc.compare(issue.severity(), approvalSeverityThreshold.get()) > 0
      );
    }

    return report.isEmpty();
  }


  /*
  *  Custom exception to keep nested if statements under control
  */
  private static class StashMissingElementException extends Exception {

    private static final long serialVersionUID = 5917014003691827699L;

    public StashMissingElementException(String exc) {
      super(exc);
    }
  }
}
