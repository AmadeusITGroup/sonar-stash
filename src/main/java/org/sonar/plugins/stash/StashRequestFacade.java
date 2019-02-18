package org.sonar.plugins.stash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.plugins.stash.StashPlugin.IssueType;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.*;
import org.sonar.plugins.stash.issue.collector.SonarQubeCollector;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;

@ScannerSide
public class StashRequestFacade implements IssuePathResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashRequestFacade.class);

  private static final String EXCEPTION_STASH_CONF = "Unable to get {0} from plugin configuration (value is null)";

  private StashPluginConfiguration config;

  private File projectBaseDir;

  public StashRequestFacade(StashPluginConfiguration stashPluginConfiguration,
      StashProjectBuilder projectBuilder) {
    this.config = stashPluginConfiguration;
    this.projectBaseDir = projectBuilder.getProjectBaseDir();
  }

  public List<PostJobIssue> extractIssueReport(Iterable<PostJobIssue> issues) {
    return SonarQubeCollector.extractIssueReport(
        issues, this, config.includeExistingIssues(), config.excludedRules()
    );
  }

  private MarkdownPrinter getMarkdownPrinter() {
    return new MarkdownPrinter(getIssueThreshold(), config.getSonarQubeURL(), config.getFilesLimitInOverview(), this);
  }

  /**
   * Post SQ analysis overview on Stash
   */
  public void postAnalysisOverview(PullRequestRef pr,
      Collection<PostJobIssue> issueReport,
      StashClient stashClient) {

    String report = getMarkdownPrinter().printReportMarkdown(issueReport);
    stashClient.postCommentOnPullRequest(pr, report);

    LOGGER.info("SonarQube analysis overview has been reported to Stash.");
  }

  /**
   * Approve pull-request
   */
  public void approvePullRequest(PullRequestRef pr, StashClient stashClient) {
    stashClient.approvePullRequest(pr);

    // squid:S2629 : no evaluation required if the logging level is not activated
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Pull-request {} ({}/{}) APPROVED by user \"{}\"",
          pr.pullRequestId(), pr.project(), pr.repository(), stashClient.getLogin());
    }
  }

  /**
   * Reset pull-request approval
   */
  public void resetPullRequestApproval(PullRequestRef pr, StashClient stashClient) {
    stashClient.resetPullRequestApproval(pr);

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("Pull-request {} ({}/{}) NOT APPROVED by user \"{}\"",
          pr.pullRequestId(), pr.project(), pr.repository(), stashClient.getLogin());
    }
  }

  /**
   * Add a reviewer to the current pull-request.
   */
  public void addPullRequestReviewer(PullRequestRef pr, String userSlug, StashClient stashClient) {
    StashPullRequest pullRequest = stashClient.getPullRequest(pr);

    // user not yet in reviewer list
    StashUser reviewer = pullRequest.getReviewer(userSlug);
    if (reviewer == null) {
      List<StashUser> reviewers = pullRequest.getReviewers();
      reviewers.add(stashClient.getUser(userSlug));

      stashClient.addPullRequestReviewer(pr, pullRequest.getVersion(), reviewers);

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("User \"{}\" is now a reviewer of the pull-request #{} in {}/{}",
            userSlug, pr.pullRequestId(), pr.project(), pr.repository());
      }
    }
  }

  /**
   * Push SonarQube report into the pull-request as comments.
   */
  public void postSonarQubeReport(PullRequestRef pr,
      Iterable<PostJobIssue> issueReport,
      StashDiffReport diffReport,
      StashClient stashClient) {
    postCommentPerIssue(pr, issueReport, diffReport, stashClient);
    LOGGER.info("New SonarQube issues (if any) have been reported to Stash.");
  }

  /**
   * Post one comment by found issue on Stash.
   */
  void postCommentPerIssue(PullRequestRef pr, Iterable<PostJobIssue> issues,
      StashDiffReport diffReport, StashClient stashClient) {

    // to optimize request to Stash, builds comment match ordered by filepath
    Map<String, StashCommentReport> commentsByFile = new HashMap<>();
    for (PostJobIssue issue : issues) {
      String path = getIssuePath(issue);
      if (commentsByFile.get(path) == null) {
        StashCommentReport comments = stashClient.getPullRequestComments(pr, path);

        // According to the type of the comment
        // if type == CONTEXT, comment.line is set to source line instead of destination line
        comments.applyDiffReport(diffReport);

        commentsByFile.put(path, comments);
      }
    }

    Severity issueSeverityThreshold = config.getIssueSeverityThreshold();
    for (PostJobIssue issue : issues) {
      if (issue.severity().compareTo(issueSeverityThreshold) >= 0) {
        postIssueComment(pr, issue, commentsByFile, diffReport, stashClient, config.getTaskIssueSeverityThreshold());
      }
    }
  }

  private void postIssueComment(PullRequestRef pr,
                                PostJobIssue issue,
                                Map<String, StashCommentReport> commentsByFile,
                                StashDiffReport diffReport,
                                StashClient stashClient,
                                Optional<Severity> taskSeverityThreshold
  ) {

    String issueKey = issue.key();
    String path = getIssuePath(issue);
    StashCommentReport comments = commentsByFile.get(path);
    String commentContent = getMarkdownPrinter().printIssueMarkdown(issue);
    Integer issueLine = issue.line();
    if (issueLine == null) {
      issueLine = 0;
    }
    // Surprisingly this syntax does not trigger the squid:NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE rule
    //    but it does if you transform that into a ternary operator at the assignment level :/

    // if comment not already pushed to Stash
    if (comments != null && comments.contains(commentContent, path, issueLine)) {
      LOGGER.debug("Comment \"{}\" already pushed on file {} ({})", issueKey, path, issueLine);
      return;
    }

    // check if issue belongs to the Stash diff view
    IssueType type = diffReport.getType(path, issueLine, config.issueVicinityRange());
    if (type == null) {
      LOGGER.info(
          "Comment \"{}\" cannot be pushed to Stash like it does not belong to diff view - {} (line: {})",
          issueKey, path, issueLine);
      return;
    }

    long line = diffReport.getLine(path, issueLine);

    StashComment comment = stashClient
        .postCommentLineOnPullRequest(pr, commentContent, path, line, type);

    LOGGER
        .info("Comment \"{}\" has been created ({}) on file {} ({})", issueKey, type, path, line);

    // Create task linked to the comment if configured

    if (
        taskSeverityThreshold.isPresent()
        && issue.severity().compareTo(taskSeverityThreshold.get()) >= 0
        ) {

      stashClient.postTaskOnComment(issue.message(), comment.getId());

      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Comment \"{}\" has been linked to a Stash task", comment.getId());
      }
    }
  }

  public StashCredentials getCredentials() {
    String passwordEnvVariable = config.getStashPasswordEnvironmentVariable();
    String password = config.getStashPassword();
    String userSlug = config.getStashUserSlug();

    if (passwordEnvVariable != null) {
      password = System.getenv(passwordEnvVariable);
      if (password == null) {
        throw new StashConfigurationException(
            "Unable to retrieve password from configured environment variable "
                + StashPlugin.STASH_PASSWORD_ENVIRONMENT_VARIABLE);
      }
    }

    if (userSlug == null) {
      userSlug = config.getStashLogin();
    }

    return new StashCredentials(config.getStashLogin(), password, userSlug);
  }

  /**
   * Mandatory Issue Threshold option.
   *
   * @throws StashConfigurationException if unable to get parameter as Integer
   */
  public int getIssueThreshold() {
    int result = 0;
    try {
      result = config.getIssueThreshold();
    } catch (NumberFormatException e) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_ISSUE_THRESHOLD
          + " from plugin configuration", e);
    }
    return result;
  }

  /**
   * Mandatory Stash URL option.
   *
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashURL() {
    String result = config.getStashURL();
    if (result == null) {
      throw new StashConfigurationException(
          MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_URL));
    }

    if (result.endsWith("/")) {
      LOGGER.warn("Stripping trailing slash from {}, as it leads to invalid URLs",
          StashPlugin.STASH_URL);
      result = StashPluginUtils.removeEnd(result, "/");
    }

    return result;
  }

  public PullRequestRef getPullRequest() {
    return PullRequestRef.builder()
        .setProject(getStashProject())
        .setRepository(getStashRepository())
        .setPullRequestId(getStashPullRequestId())
        .build();
  }

  /**
   * Mandatory Stash Project option.
   *
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashProject() {
    String result = config.getStashProject();
    if (result == null) {
      throw new StashConfigurationException(
          MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_PROJECT));
    }

    return result;
  }

  /**
   * Mandatory Stash Repository option.
   *
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashRepository() {
    String result = config.getStashRepository();
    if (result == null) {
      throw new StashConfigurationException(
          MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_REPOSITORY));
    }

    return result;
  }

  /**
   * Mandatory Stash pull-request ID option.
   *
   * @throws StashConfigurationException if unable to get parameter
   */
  public int getStashPullRequestId() {
    Integer result = config.getPullRequestId();
    if (result == null) {
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF,
          StashPlugin.STASH_PULL_REQUEST_ID));
    }

    return result;
  }

  /**
   * Get user who published the SQ analysis in Stash.
   */
  public StashUser getSonarQubeReviewer(String userSlug, StashClient stashClient) {
    StashUser result = null;

    result = stashClient.getUser(userSlug);

    LOGGER.debug("SonarQube reviewer {} identified in Stash", userSlug);
    return result;
  }

  /**
   * Get all changes exposed through the Stash pull-request.
   */
  public StashDiffReport getPullRequestDiffReport(PullRequestRef pr, StashClient stashClient) {
    StashDiffReport result = stashClient.getPullRequestDiffs(pr);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Stash differential report retrieved from pull request {} #{}",
          pr.repository(), pr.pullRequestId());
    }

    return result;
  }

  /**
   * Reset all comments linked to a pull-request.
   */
  public void resetComments(PullRequestRef pr,
      StashDiffReport diffReport,
      StashUser sonarUser,
      StashClient stashClient) {
    // Let's call this "diffRep_loop"
    for (StashComment comment : diffReport.getComments()) {

      // delete comment only if published by the current SQ user
      if (sonarUser.getId() != comment.getAuthor().getId()) {
        continue;
        // Next element in "diffRep_loop"

        // comment contains tasks which cannot be deleted => do nothing
      } else if (comment.containsPermanentTasks()) {
        LOGGER.debug("Comment \"{}\" (path:\"{}\", line:\"{}\")"
                + "CANNOT be deleted because one of its tasks is not deletable.", comment.getId(),
            comment.getPath(),
            comment.getLine());
        continue;  // Next element in "diffRep_loop"
      }

      // delete tasks linked to the current comment
      for (StashTask task : comment.getTasks()) {
        stashClient.deleteTaskOnComment(task);
      }

      stashClient.deletePullRequestComment(pr, comment);
    }

    LOGGER.info("SonarQube issues reported to Stash by user \"{}\" have been reset",
        sonarUser.getName());
  }

  @Override
  public String getIssuePath(PostJobIssue issue) {
    InputComponent ip = issue.inputComponent();
    if (ip == null || !ip.isFile()) {
      return null;
    }
    InputFile inputFile = (InputFile) ip;

    Path baseDir = config
        .getRepositoryRoot()
        .orElse(projectBaseDir).toPath();


    return new PathResolver().relativePath(baseDir, inputFile.path());
  }
}
