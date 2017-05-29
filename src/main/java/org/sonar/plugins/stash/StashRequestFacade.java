package org.sonar.plugins.stash;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.coverage.CoverageProjectStore;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.MarkdownPrinter;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;
import org.sonar.plugins.stash.issue.collector.SonarQubeCollector;


public class StashRequestFacade implements BatchComponent, IssuePathResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashRequestFacade.class);

  private static final String EXCEPTION_STASH_CONF = "Unable to get {0} from plugin configuration (value is null)";
  private static final String STACK_TRACE = "Exception stack trace";

  private StashPluginConfiguration config;
  private File projectBaseDir;
  private final InputFileCache inputFileCache;
  private CoverageProjectStore coverageProjectStore;

  public StashRequestFacade(StashPluginConfiguration stashPluginConfiguration, InputFileCache inputFileCache, StashProjectBuilder projectBuilder, CoverageProjectStore coverageProjectStore) {
    this.config = stashPluginConfiguration;
    this.inputFileCache = inputFileCache;
    this.projectBaseDir = projectBuilder.getProjectBaseDir();
    this.coverageProjectStore = coverageProjectStore;
  }

  public List<Issue> extractIssueReport(ProjectIssues projectIssues) {
    return SonarQubeCollector.extractIssueReport(projectIssues, this);
  }

  /**
   * Post SQ analysis overview on Stash
   */
  public void postAnalysisOverview(PullRequestRef pr, int issueThreshold, List<Issue> issueReport,
                                   StashClient stashClient) {

    try {
      String report = MarkdownPrinter.printReportMarkdown(
              pr, stashClient.getBaseUrl(), config.getSonarQubeURL(), issueReport, issueThreshold,
              coverageProjectStore.getProjectCoverage(), coverageProjectStore.getPreviousProjectCoverage(),
              this
      );
      stashClient.postCommentOnPullRequest(pr, report);

      LOGGER.info("SonarQube analysis overview has been reported to Stash.");

    } catch (StashClientException e) {
      LOGGER.error("Unable to push SonarQube analysis overview to Stash", e);
    }
  }

  /**
   * Approve pull-request
   */
  public void approvePullRequest(PullRequestRef pr, StashClient stashClient) {
    try {
      stashClient.approvePullRequest(pr);

      LOGGER.info("Pull-request {} ({}/{}) APPROVED by user \"{}\"",
        pr.pullRequestId(), pr.project(), pr.repository(), stashClient.getLogin());

    } catch (StashClientException e) {
      LOGGER.error("Unable to approve pull-request", e);
    }
  }

  /**
   * Reset pull-request approval
   */
  public void resetPullRequestApproval(PullRequestRef pr, StashClient stashClient) {
    try {
      stashClient.resetPullRequestApproval(pr);

      LOGGER.info("Pull-request {} ({}/{}) NOT APPROVED by user \"{}\"",
              pr.pullRequestId(), pr.project(), pr.repository(), stashClient.getLogin());

    } catch (StashClientException e) {
      LOGGER.error("Unable to reset pull-request approval", e);
    }
  }

  /**
   * Add a reviewer to the current pull-request.
   */
  public void addPullRequestReviewer(PullRequestRef pr, String user, StashClient stashClient) {
    try {
      StashPullRequest pullRequest = stashClient.getPullRequest(pr);

      // user not yet in reviewer list
      StashUser reviewer = pullRequest.getReviewer(user);
      if (reviewer == null) {
        ArrayList<StashUser> reviewers = new ArrayList<>(pullRequest.getReviewers());
        reviewers.add(stashClient.getUser(user));

        stashClient.addPullRequestReviewer(pr, pullRequest.getVersion(), reviewers);

        LOGGER.info("User \"{}\" is now a reviewer of the pull-request {} #{}", user, pr.pullRequestId(), pr.project(), pr.repository());
      }
    } catch (StashClientException e) {
      LOGGER.error("Unable to add a new reviewer to the pull-request", e);
    }
  }

  /**
   * Push SonarQube report into the pull-request as comments.
   */
  public void postSonarQubeReport(PullRequestRef pr, List<Issue> issueReport, StashDiffReport diffReport, StashClient stashClient) {
    try {
      postCommentPerIssue(pr, issueReport, diffReport, stashClient);

      LOGGER.info("New SonarQube issues have been reported to Stash.");

    } catch (StashClientException e) {
      LOGGER.error("Unable to link SonarQube issues to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }

  /**
   * Post one comment by found issue on Stash.
   */
  void postCommentPerIssue(PullRequestRef pr, Collection<Issue> issues, StashDiffReport diffReport, StashClient stashClient) throws StashClientException {

    // to optimize request to Stash, builds comment match ordered by filepath
    Map<String, StashCommentReport> commentsByFile = new HashMap<>();
    for (Issue issue : issues) {
      String path = getIssuePath(issue);
      if (commentsByFile.get(path) == null) {
        StashCommentReport comments = stashClient.getPullRequestComments(pr, path);

        // According to the type of the comment
        // if type == CONTEXT, comment.line is set to source line instead of destination line
        comments.applyDiffReport(diffReport);

        commentsByFile.put(path, comments);
      }
    }

    // Severity available to create a task
    List<String> taskSeverities = getReportedSeverities();

    for (Issue issue : issues) {
        postIssueComment(pr, issue, commentsByFile, diffReport, stashClient, taskSeverities);
    }
  }

  private void postIssueComment(PullRequestRef pr, Issue issue, Map<String, StashCommentReport> commentsByFile,
                                StashDiffReport diffReport, StashClient stashClient, List<String> taskSeverities)
                                throws StashClientException {
    String path = getIssuePath(issue);
    StashCommentReport comments = commentsByFile.get(path);
    String commentContent = MarkdownPrinter.printIssueMarkdown(issue, config.getSonarQubeURL());
    Integer issueLine = issue.line();
    // FIXME move this somewhere else
    if (issueLine == null) {
      issueLine = 0;
    }

    // if comment not already pushed to Stash
    if (comments != null && comments.contains( commentContent, path, issueLine)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Comment \"{}\" already pushed on file {} ({})", issue.key(),
                path, issueLine);
      }
      return;
    }

    // check if issue belongs to the Stash diff view
    String type = diffReport.getType(path, issueLine);
    if (type == null) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("Comment \"{}\" cannot be pushed to Stash like it does not belong to diff view - {} (line: {})",
                issue.key(), path, issueLine);
      }
      return;
    }

    long line = diffReport.getLine(path, issueLine);

    StashComment comment = stashClient.postCommentLineOnPullRequest(pr,
            commentContent, path, line, type);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Comment \"{}\" has been created ({}) on file {} ({})", issue.key(), type,
              path, line);
    }

    // Create task linked to the comment if configured
    if (taskSeverities.contains(issue.severity())) {
      stashClient.postTaskOnComment(issue.message(), comment.getId());

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Comment \"{}\" has been linked to a Stash task", comment.getId());
      }
    }

  }

  public StashCredentials getCredentials() throws StashConfigurationException {
    String passwordEnvVariable = config.getStashPasswordEnvironmentVariable();
    String password = config.getStashPassword();
    if (passwordEnvVariable != null) {
      password = System.getenv(passwordEnvVariable);
      if (password == null) {
        throw new StashConfigurationException(
                "Unable to retrieve password from configured environment variable " +
                        StashPlugin.STASH_PASSWORD_ENVIRONMENT_VARIABLE);
      }
    }
    return new StashCredentials(config.getStashLogin(), password);
  }

  /**
   * Mandatory Issue Threshold option.
   *
   * @throws StashConfigurationException if unable to get parameter as Integer
   */
  public int getIssueThreshold() throws StashConfigurationException {
    int result = 0;
    try {
      result = config.getIssueThreshold();
    } catch (NumberFormatException e) {
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_ISSUE_THRESHOLD + " from plugin configuration", e);
    }
    return result;
  }

  /**
   * Mandatory Stash URL option.
   *
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashURL() throws StashConfigurationException {
    String result = config.getStashURL();
    if (result == null) {
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_URL));
    }

    if (result.endsWith("/")) {
      LOGGER.warn("Stripping trailing slash from {}, as it leads to invalid URLs", StashPlugin.STASH_URL);
      result = StringUtils.removeEnd(result, "/");
    }

    return result;
  }

  public PullRequestRef getPullRequest(StashClient stashClient) throws StashConfigurationException {
    return PullRequestRef.builder()
            .setProject(getStashProject())
            .setRepository(getStashRepository())
            .setPullRequestId(getStashPullRequestId(stashClient))
            .build();
  }

  /**
   * Mandatory Stash Project option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashProject() throws StashConfigurationException {
    String result = config.getStashProject();
    if (result == null){
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_PROJECT));
    }
    
    return result;
  }
  
  /**
   * Mandatory Stash Repository option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashRepository() throws StashConfigurationException {
    String result = config.getStashRepository();
    if (result == null){
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_REPOSITORY));
    }
    
    return result;
  }

  /**
   * Gets or resolves pull request id
   * If sonar.stash.pullrequest.id specified returns it's value
   * Otherwise takes branch name from sonar.stash.branch or sonar.branch property in this precedence
   * and resolves pull request id using Stash REST API
   * @param stashClient - client
   * @return pull request id
   * @throws StashConfigurationException
   */
  public int getStashPullRequestId(StashClient stashClient) throws StashConfigurationException {
    Integer result = config.getPullRequestId();
    if (result != null && result != 0){
      return result;
    }
    // try to get PR id from branch name
    String branchName = config.getSonarStashBranch();
    if (branchName == null) {
      branchName = config.getSonarBranch();
    }
    if (branchName == null) {
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF,
              StashPlugin.STASH_BRANCH_NAME + " or " + CoreProperties.PROJECT_BRANCH_PROPERTY));
    }

    try {
      result = stashClient.getPullRequestId(getStashProject(), getStashRepository(), branchName);
    } catch (StashClientException e) {
      LOGGER.error("Error when trying to resolve PR id", e);
    }
    if (result == null || result == 0) {
      throw new StashConfigurationException("Unable to find pull request for branch " + branchName);
    }
    return result;
  }

  /**
   * Get user who published the SQ analysis in Stash.
   */
  public StashUser getSonarQubeReviewer(String user, StashClient stashClient){
    StashUser result = null;
    
    try {
      result = stashClient.getUser(user);
      
      LOGGER.debug("SonarQube reviewer {} identified in Stash", user);
      
    } catch(StashClientException e){
        LOGGER.error("Unable to get SonarQube reviewer from Stash", e);
    }
    
    return result;
  }
  
  /**
   * Get all changes exposed through the Stash pull-request.
   */
  public StashDiffReport getPullRequestDiffReport(PullRequestRef pr, StashClient stashClient){
    StashDiffReport result = null;
    
    try {
      result = stashClient.getPullRequestDiffs(pr);
      
      LOGGER.debug("Stash differential report retrieved from pull request {} #{}", pr.repository(), pr.pullRequestId());
      
    } catch(StashClientException e){
        LOGGER.error("Unable to get Stash differential report from Stash", e);
    }
    
    return result;
  }
  
  /**
   * Reset all comments linked to a pull-request.
   */
  public void resetComments(PullRequestRef pr, StashDiffReport diffReport, StashUser sonarUser, StashClient stashClient) {
    try {
      // Let's call this "diffRep_loop"
      for (StashComment comment : diffReport.getComments()) {
        
        // delete comment only if published by the current SQ user
        if (sonarUser.getId() != comment.getAuthor().getId()) {
          continue;
          // Next element in "diffRep_loop"

        // comment contains tasks which cannot be deleted => do nothing
        } else if (comment.containsPermanentTasks()) {
          LOGGER.debug("Comment \"{}\" (path:\"{}\", line:\"{}\")" +
                       "CANNOT be deleted because one of its tasks is not deletable.", comment.getId(),
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
      
      LOGGER.info("SonarQube issues reported to Stash by user \"{}\" have been reset", sonarUser.getName());
      
    } catch (StashClientException e){
        LOGGER.error("Unable to reset comment list", e);
    }
  }
  
  /**
   * Get reported severities to create a task.
   */
  public List<String> getReportedSeverities() {
      List<String> result = new ArrayList<>();
      String threshold = config.getTaskIssueSeverityThreshold();
        
      // threshold == NONE, no severities reported 
      if (! StringUtils.equals(threshold, StashPlugin.SEVERITY_NONE)) {
        
        // INFO, MINOR, MAJOR, CRITICAL, BLOCKER
        boolean hit = false;
        for (String severity : StashPlugin.SEVERITY_LIST) {
          
          if (hit || StringUtils.equals(severity, threshold)) {
            result.add(severity);
            hit = true;
          }
        }
      }
      
      return result;
  }

  // this lives here, as we need access to both the InputFileCache and the StashRequestFacade
  @Override
  public String getIssuePath(Issue issue) {
    InputFile inputFile = inputFileCache.getInputFile(issue.componentKey());
    if (inputFile == null){
      return null;
    }

    File baseDir = config
            .getRepositoryRoot()
            .orElse(projectBaseDir);

    return new PathResolver().relativePath(baseDir, inputFile.file());
  }
}
