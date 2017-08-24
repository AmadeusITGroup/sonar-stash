package org.sonar.plugins.stash;

import java.util.Optional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.platform.Server;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;

@BatchSide
public class StashIssueReportingPostJob implements PostJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashIssueReportingPostJob.class);
  private static final String STACK_TRACE = "Exception stack trace";

  private final StashPluginConfiguration config;
  private final StashRequestFacade stashRequestFacade;
  private final Server sonarQubeServer;

  public StashIssueReportingPostJob(StashPluginConfiguration stashPluginConfiguration,
      StashRequestFacade stashRequestFacade,
      Server sonarQubeServer) {
    this.config = stashPluginConfiguration;
    this.stashRequestFacade = stashRequestFacade;
    this.sonarQubeServer = sonarQubeServer;
  }

  @Override
  public void execute(PostJobContext context) {
    if (!config.hasToNotifyStash()) {
      LOGGER.info("{} not enabled, skipping", this);
      return;
    }

    try {
      // Stash MANDATORY options
      String stashURL = stashRequestFacade.getStashURL();
      int stashTimeout = config.getStashTimeout();

      StashCredentials stashCredentials = stashRequestFacade.getCredentials();

      try (StashClient stashClient = new StashClient(stashURL,
          stashCredentials,
          stashTimeout,
          sonarQubeServer.getVersion())) {

        // Down the rabbit hole...
        updateStashWithSonarInfo(stashClient, stashCredentials, context.issues());
      }
    } catch (StashConfigurationException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }


  /*
  * Second part of the code necessary for the executeOn() -- squid:S134
  */
  private void updateStashWithSonarInfo(StashClient stashClient,
      StashCredentials stashCredentials, Iterable<PostJobIssue> issues) {

    try {
      int issueThreshold = stashRequestFacade.getIssueThreshold();
      PullRequestRef pr = stashRequestFacade.getPullRequest();

      // SonarQube objects
      List<PostJobIssue> issueReport = stashRequestFacade.extractIssueReport(issues);

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
  private void postInfoAndPRsActions(
      PullRequestRef pr, List<PostJobIssue> issueReport, int issueThreshold,
      StashDiffReport diffReport, StashClient stashClient
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
      stashRequestFacade.postAnalysisOverview(pr, issueReport, stashClient);
    }

    if (config.canApprovePullRequest()) {
      if (shouldApprovePullRequest(config.getApprovalSeverityThreshold(), issueReport)) {
        stashRequestFacade.approvePullRequest(pr, stashClient);
      } else {
        stashRequestFacade.resetPullRequestApproval(pr, stashClient);
      }
    }
  }

  static boolean shouldApprovePullRequest(Optional<Severity> approvalSeverityThreshold, List<PostJobIssue> report) {
    if (approvalSeverityThreshold.isPresent()) {
      return report.stream().noneMatch(issue ->
          issue.severity().compareTo(approvalSeverityThreshold.get()) > 0
      );
    }

    return report.isEmpty();
  }

  @Override
  public void describe(PostJobDescriptor descriptor) {
    descriptor.requireProperty(StashPlugin.STASH_NOTIFICATION);
    descriptor.name("Stash/Bitbucket notification");
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
