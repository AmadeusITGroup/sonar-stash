package org.sonar.plugins.stash;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.*;
import org.sonar.plugins.stash.issue.collector.SonarQubeCollector;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class StashRequestFacade implements BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashRequestFacade.class);

  private StashPluginConfiguration config;
  private File projectBaseDir;

  public StashRequestFacade(StashPluginConfiguration stashPluginConfiguration) {
    this.config = stashPluginConfiguration;
  }

  public void initialize(File projectBaseDir) {
    this.projectBaseDir = projectBaseDir;
  }

  public SonarQubeIssuesReport extractIssueReport(ProjectIssues projectIssues, InputFileCache inputFileCache) {
    return SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
  }

  /**
   * Post SQ analysis overview on Stash
   */
  public void postAnalysisOverview(String project, String repository, String pullRequestId, String sonarQubeURL, int issueThreshold, SonarQubeIssuesReport issueReport,
    StashClient stashClient) {
    try {
      stashClient.postCommentOnPullRequest(project,
        repository,
        pullRequestId,
        MarkdownPrinter.printReportMarkdown(issueReport, sonarQubeURL, issueThreshold));

      LOGGER.info("SonarQube analysis overview has been reported to Stash.");

    } catch (StashClientException e) {
      LOGGER.error("Unable to push SonarQube analysis overview to Stash: {}", e.getMessage());
      LOGGER.debug("Exception stack trace", e);
    }
  }

  /**
   * Create Stash comments for SonarQube issues.
   */
  public void postCommentPerIssue(String project, String repository, String pullRequestId, String sonarQubeURL, SonarQubeIssuesReport issuesReport, StashClient stashClient) {
    try {
      StashDiffReport diffReport = stashClient.getPullRequestDiffs(project, repository, pullRequestId);
      Map<String, StashCommentReport> commentsBySonarQubeFilePath = getStashCommentsBySonarQubeFilePath(project, repository, pullRequestId, issuesReport, stashClient, diffReport);
      String issueSonarQubeFilePath;
      String issueStashFilePath;

      for (SonarQubeIssue issue : issuesReport.getIssues()) {
        issueSonarQubeFilePath = issue.getPath();
        issueStashFilePath = diffReport.getPath(issue.getPath());

        StashCommentReport comments = commentsBySonarQubeFilePath.get(issueSonarQubeFilePath);

        // If the Stash comment (SonarQube issue) is already present in the pull request (from previous analysis), do not create again this
        // Stash comment.
        if (comments != null
          && comments.contains(MarkdownPrinter.printIssueMarkdown(issue, sonarQubeURL), issueStashFilePath, issue.getLine())) {
          LOGGER.debug("Stash comment for SonarQube issue \"{}\" is already present on file \"{}\" at line {}.", issue.getRule(), issueStashFilePath, issue.getLine());
          continue;
        }

        // If the SonarQube issue does not belong to the Stash diff view, do not create a Stash comment for this issue.
        if (diffReport.getType(issueStashFilePath, issue.getLine()) == null) {
          LOGGER.debug("Stash comment for SonarQube issue \"{}\" cannot be created because the issue does not belong to diff view \"{}\", line {})", issue.getRule(),
            issueSonarQubeFilePath, issue.getLine());
          continue;
        }

        // Create the Stash comments for the SonarQube issues.
        stashClient.postCommentLineOnPullRequest(
          project,
          repository,
          pullRequestId,
          MarkdownPrinter.printIssueMarkdown(issue, sonarQubeURL),
          issueStashFilePath,
          diffReport.getLine(issueStashFilePath, issue.getLine()),
          diffReport.getType(issueStashFilePath, issue.getLine())
          );
        LOGGER.debug("Stash comment \"{}\" has been created ({}) on file \"{}\" at line {}", issue.getRule(), diffReport.getType(issueStashFilePath, issue.getLine()),
          issueStashFilePath, diffReport.getLine(issueStashFilePath, issue.getLine()));
      }

      LOGGER.info("All Stash comments for SonarQube issues have been created.");

    } catch (StashClientException e) {
      LOGGER.error("Unable to link SonarQube issues to Stash: {}", e.getMessage());
      LOGGER.debug("Exception stack trace", e);
    }
  }

  public StashCredentials getCredentials() {
    return new StashCredentials(config.getStashLogin(), config.getStashPassword());
  }

  /**
   * Mandatory Issue Threshold option.
   * @throws StashConfigurationException if unable to get parameter as Integer
   */
  public int getIssueThreshold() throws StashConfigurationException {
    int result;
    try {
      result = config.getIssueThreshold();
    } catch (NumberFormatException e) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_ISSUE_THRESHOLD + " from plugin configuration", e);
    }
    return result;
  }

  /**
   * Mandatory Stash URL option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashURL() throws StashConfigurationException {
    String result = config.getStashURL();
    if (result == null) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_URL + " from plugin configuration (value is null)");
    }
    return result;
  }

  /**
   * Mandatory Stash Project option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashProject() throws StashConfigurationException {
    String result = config.getStashProject();
    if (result == null) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_PROJECT + " (value is null)");
    }
    return result;
  }

  /**
   * Mandatory Stash Repository option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashRepository() throws StashConfigurationException {
    String result = config.getStashRepository();
    if (result == null) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_REPOSITORY + " (value is null)");
    }
    return result;
  }

  /**
   * Mandatory Stash pull-request ID option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashPullRequestId() throws StashConfigurationException {
    String result = config.getPullRequestId();
    if (result == null) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_PULL_REQUEST_ID + ": value is null");
    }
    return result;
  }

  /**
   * To optimize requests to Stash, group Stash comments by SonarQube file path.
   */
  private static Map<String, StashCommentReport> getStashCommentsBySonarQubeFilePath(String project, String repository, String pullRequestId, SonarQubeIssuesReport issuesReport,
    StashClient stashClient, StashDiffReport diffReport) throws StashClientException {
    Map<String, StashCommentReport> commentsBySonarQubeFilePath = new HashMap();
    String issueSonarQubeFilePath;
    String issueStashFilePath;
    for (SonarQubeIssue issue : issuesReport.getIssues()) {
      issueSonarQubeFilePath = issue.getPath();
      issueStashFilePath = diffReport.getPath(issueSonarQubeFilePath);
      if (commentsBySonarQubeFilePath.get(issueSonarQubeFilePath) == null) {
        StashCommentReport comments = stashClient.getPullRequestComments(project, repository, pullRequestId, issueStashFilePath);
        comments.applyDiffReport(diffReport);
        commentsBySonarQubeFilePath.put(issueSonarQubeFilePath, comments);
      }
    }
    return commentsBySonarQubeFilePath;
  }

}
