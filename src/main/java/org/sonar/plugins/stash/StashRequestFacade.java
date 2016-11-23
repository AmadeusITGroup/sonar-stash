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
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.plugins.stash.client.SonarQubeClient;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.SonarQubeClientException;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.CoverageIssuesReport;
import org.sonar.plugins.stash.issue.Issue;
import org.sonar.plugins.stash.issue.MarkdownPrinter;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;
import org.sonar.plugins.stash.issue.collector.SonarQubeCollector;


@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class StashRequestFacade implements BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(StashRequestFacade.class);

  private static final String EXCEPTION_STASH_CONF = "Unable to get {0} from plugin configuration (value is null)";
  private static final String STACK_TRACE = "Exception stack trace";
  
  private StashPluginConfiguration config;
  private File projectBaseDir;
  
  public StashRequestFacade(StashPluginConfiguration stashPluginConfiguration) {
    this.config = stashPluginConfiguration;
  }
  
  public void initialize(File projectBaseDir) {
    this.projectBaseDir = projectBaseDir;
  }
  
  public SonarQubeIssuesReport extractIssueReport(ProjectIssues projectIssues, InputFileCache inputFileCache){
    return SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
  }

  /**
   * Post SQ analysis overview on Stash
   */
  public void postAnalysisOverview(String[] idCard, int issueThreshold, SonarQubeIssuesReport issueReport,
                                   CoverageIssuesReport coverageReport, StashClient stashClient){

    // Splitting the idCard into variables
    String project       = idCard[0];
    String repository    = idCard[1];
    String pullRequestId = idCard[2];
    String sonarQubeURL  = idCard[3];
    String stashURL      = idCard[4];

    try {
      stashClient.postCommentOnPullRequest(project,
                                         repository,
                                         pullRequestId,
                                         MarkdownPrinter.printReportMarkdown(project, repository, pullRequestId, issueReport, coverageReport, sonarQubeURL, stashURL, issueThreshold));
    
      LOGGER.info("SonarQube analysis overview has been reported to Stash.");
      
    } catch(StashClientException e){
        LOGGER.error("Unable to push SonarQube analysis overview to Stash", e);
    }
  }
  
  /**
   * Approve pull-request
   */
  public void approvePullRequest(String project, String repository, String pullRequestId, String user, StashClient stashClient){
    try {
      stashClient.approvePullRequest(project, repository, pullRequestId);
      
      LOGGER.info("Pull-request {} ({}/{}) APPROVED by user \"{}\"", pullRequestId, project, repository, user);
      
    } catch(StashClientException e){
        LOGGER.error("Unable to approve pull-request", e);
    }
  }
  
  /**
   * Reset pull-request approval
   */
  public void resetPullRequestApproval(String project, String repository, String pullRequestId, String user, StashClient stashClient){
    try {
      stashClient.resetPullRequestApproval(project, repository, pullRequestId);
      
      LOGGER.info("Pull-request {} ({}/{}) NOT APPROVED by user \"{}\"", pullRequestId, project, repository, user);
      
    } catch(StashClientException e){
        LOGGER.error("Unable to reset pull-request approval", e);
    }
  }
  
  /**
   * Add a reviewer to the current pull-request.
   */
  public void addPullRequestReviewer(String project, String repository, String pullRequestId, String user, StashClient stashClient){
    try {
      StashPullRequest pullRequest = stashClient.getPullRequest(project, repository, pullRequestId);
      
      // user not yet in reviewer list
      StashUser reviewer = pullRequest.getReviewer(user);
      if (reviewer == null) {
        ArrayList<StashUser> reviewers = new ArrayList<>(pullRequest.getReviewers());
        reviewers.add(stashClient.getUser(user));
        
        stashClient.addPullRequestReviewer(project, repository, pullRequestId, pullRequest.getVersion(), reviewers);
      
        LOGGER.info("User \"{}\" is now a reviewer of the pull-request {} #{}", user, pullRequestId, project, repository);
      }
    } catch(StashClientException e){
        LOGGER.error("Unable to add a new reviewer to the pull-request", e);
    }
  }
  
  /**
   * Push SonarQube report into the pull-request as comments.
   */
  public void postSonarQubeReport(String project, String repository, String pullRequestId, String sonarQubeURL, SonarQubeIssuesReport issueReport, StashDiffReport diffReport, StashClient stashClient) {
    try {
      postCommentPerIssue(project, repository, pullRequestId, sonarQubeURL, issueReport.getIssues(), diffReport, stashClient);
      
      LOGGER.info("New SonarQube issues have been reported to Stash.");
      
    } catch (StashClientException e){
      LOGGER.error("Unable to link SonarQube issues to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }
  
  /**
   * Push Code Coverage report into the pull-request as comments.
   */
  public void postCoverageReport(String project, String repository, String pullRequestId, String sonarQubeURL, CoverageIssuesReport coverageReport, StashDiffReport diffReport, StashClient stashClient) {
    try {
      if (! coverageReport.isEmpty()) {
        
        postCommentPerIssue(project, repository, pullRequestId, sonarQubeURL, coverageReport.getLoweredIssues(), diffReport, stashClient);
      
        LOGGER.info("Code coverage report has been reported to Stash.");
      }
    } catch (StashClientException e){
      LOGGER.error("Unable to push code coverage report to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
  }
  
  /**
   * Post one comment by found issue on Stash.
   * @throws StashClientException 
   */
  void postCommentPerIssue(String project, String repository, String pullRequestId, String sonarQubeURL,
      Collection issues, StashDiffReport diffReport, StashClient stashClient) throws StashClientException {
    
    // to optimize request to Stash, builds comment match ordered by filepath
    Map<String,StashCommentReport> commentsByFile = new HashMap<>();
    for (Object object : issues) {
      Issue issue = (Issue) object;
      
      if (commentsByFile.get(issue.getPath()) == null){
        StashCommentReport comments = stashClient.getPullRequestComments(project, repository, pullRequestId, issue.getPath());
          
        // According to the type of the comment
        // if type == CONTEXT, comment.line is set to source line instead of destination line
        comments.applyDiffReport(diffReport);
          
        commentsByFile.put(issue.getPath(), comments);
      }
    }
        
    try {
      // Severity available to create a task
      List<String> taskSeverities = getReportedSeverities();

      // Let's call this "issue_loop"
      for (Object object : issues) {
        Issue issue = (Issue) object;
        StashCommentReport comments = commentsByFile.get(issue.getPath());
        
        // if comment not already pushed to Stash
        if ((comments != null) &&
            (comments.contains(issue.printIssueMarkdown(sonarQubeURL), issue.getPath(), issue.getLine()))) {
          LOGGER.debug("Comment \"{}\" already pushed on file {} ({})", issue.getKey(),
                                                                        issue.getPath(), issue.getLine());
          continue;  // Next element in "issue_loop"
        }
        
        // check if issue belongs to the Stash diff view
        String type = diffReport.getType(issue.getPath(), issue.getLine());
        if (type == null) {
          LOGGER.info("Comment \"{}\" cannot be pushed to Stash like it does not belong to diff view - {} (line: {})",
                                                                   issue.getKey(), issue.getPath(), issue.getLine());
          continue;  // Next element in "issue_loop"
        }

        long line = diffReport.getLine(issue.getPath(), issue.getLine());

        StashComment comment = stashClient.postCommentLineOnPullRequest(project,
                                                                       repository,
                                                                       pullRequestId,
                                                                       issue.printIssueMarkdown(sonarQubeURL),
                                                                       issue.getPath(),
                                                                       line,
                                                                       type);
  
        LOGGER.debug("Comment \"{}\" has been created ({}) on file {} ({})", issue.getKey(), type,
                                                                             issue.getPath(), line);

        // Create task linked to the comment if configured
        if (taskSeverities.contains(issue.getSeverity())) {
          stashClient.postTaskOnComment(issue.getMessage(), comment.getId());

          LOGGER.debug("Comment \"{}\" has been linked to a Stash task", comment.getId());
        }


      }
      
    } catch (StashClientException e){
        LOGGER.error("Unable to link SonarQube issues to Stash", e);
    }
  }
  
  public StashCredentials getCredentials(){
    return new StashCredentials(config.getStashLogin(), config.getStashPassword());
  }
  
  /**
   * Mandatory Issue Threshold option.
   * @throws StashConfigurationException if unable to get parameter as Integer
   */
  public int getIssueThreshold() throws StashConfigurationException {
    int result = 0;
    try {
      result = config.getIssueThreshold();
    } catch(NumberFormatException e) {
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
    if (result == null){
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_URL));
    }

    if (result.endsWith("/")) {
      LOGGER.warn("Stripping trailing slash from {}, as it leads to invalid URLs", StashPlugin.STASH_URL);
      result = StringUtils.removeEnd(result, "/");
    }

    return result;
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
   * Mandatory Stash pull-request ID option.
   * @throws StashConfigurationException if unable to get parameter
   */
  public String getStashPullRequestId() throws StashConfigurationException {
    String result = config.getPullRequestId();
    if (result == null){
      throw new StashConfigurationException(MessageFormat.format(EXCEPTION_STASH_CONF, StashPlugin.STASH_PULL_REQUEST_ID));
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
  public StashDiffReport getPullRequestDiffReport(String project, String repository,
                                                  String pullRequestId, StashClient stashClient){
    StashDiffReport result = null;
    
    try {
      result = stashClient.getPullRequestDiffs(project, repository, pullRequestId);
      
      LOGGER.debug("Stash differential report retrieved from pull request {} #{}", repository, pullRequestId);
      
    } catch(StashClientException e){
        LOGGER.error("Unable to get Stash differential report from Stash", e);
    }
    
    return result;
  }
  
  /**
   * Reset all comments linked to a pull-request.
   */
  public void resetComments(String project, String repository, String pullRequestId,
                            StashDiffReport diffReport, StashUser sonarUser, StashClient stashClient) {
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

        stashClient.deletePullRequestComment(project, repository, pullRequestId, comment);
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
  
  /**
   * Get severity to associate to Code Coverage issues.
   */
  public String getCodeCoverageSeverity() {
    return config.getCodeCoverageSeverity();
  }
  
  /**
   * Extract Code Coverage report to be published into the pull-request.
   */
  public CoverageIssuesReport getCoverageReport(String sonarQubeProjectKey, SensorContext context,
      InputFileCacheSensor inputFileCacheSensor, String codeCoverageSeverity, SonarQubeClient sonarqubeClient) {
    
    CoverageIssuesReport result = new CoverageIssuesReport();
    
    try {
      result = SonarQubeCollector.extractCoverageReport(sonarQubeProjectKey, context, inputFileCacheSensor, codeCoverageSeverity, sonarqubeClient);
    
    } catch (SonarQubeClientException e) {
      LOGGER.error("Unable to push SonarQube report to Stash: {}", e.getMessage());
      LOGGER.debug(STACK_TRACE, e);
    }
    
    return result;
  }
  
}
