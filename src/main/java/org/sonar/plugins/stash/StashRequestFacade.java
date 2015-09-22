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
import org.sonar.plugins.stash.issue.MarkdownPrinter;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
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
  
  public SonarQubeIssuesReport extractIssueReport(ProjectIssues projectIssues, InputFileCache inputFileCache){
    return SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
  }

  /**
   * Post SQ analysis overview on Stash
   */
  public void postAnalysisOverview(String project, String repository, String pullRequestId, String sonarQubeURL, int issueThreshold, SonarQubeIssuesReport issueReport, StashClient stashClient){
    try {
      stashClient.postCommentOnPullRequest(project,
                                         repository,
                                         pullRequestId,
                                         MarkdownPrinter.printReportMarkdown(issueReport, sonarQubeURL, issueThreshold));
    
      LOGGER.info("SonarQube analysis overview has been reported to Stash.");
      
    } catch(StashClientException e){
      LOGGER.error("Unable to push SonarQube analysis overview to Stash: {}", e.getMessage());
      LOGGER.debug("Exception stack trace", e);
    }
  }
  
  /**
   * Post one comment by found issue on Stash.
   */
  public void postCommentPerIssue(String project, String repository, String pullRequestId, String sonarQubeURL, SonarQubeIssuesReport issueReport, StashClient stashClient){
    try {
      // get all diff associated to current pull-request
      StashDiffReport diffReport = stashClient.getPullRequestDiffs(project, repository, pullRequestId);
      
      // to optimize request to Stash, builds comment match ordered by filepath
      Map<String,StashCommentReport> commentsByFile = new HashMap<String, StashCommentReport>();
      for (SonarQubeIssue issue : issueReport.getIssues()) {
        if (commentsByFile.get(issue.getPath()) == null){
          StashCommentReport comments = stashClient.getPullRequestComments(project, repository, pullRequestId, issue.getPath());
          
          // According to the type of the comment
          // if type == CONTEXT, comment.line is set to source line instead of destination line
          comments.applyDiffReport(diffReport);
          
          commentsByFile.put(issue.getPath(), comments);
        }
      }
      
      for (SonarQubeIssue issue : issueReport.getIssues()) {
        StashCommentReport comments = commentsByFile.get(issue.getPath());
        
        // if comment not already pushed to Stash
        if ((comments != null) &&
            (comments.contains(MarkdownPrinter.printIssueMarkdown(issue, sonarQubeURL), issue.getPath(), issue.getLine()))) {
          LOGGER.debug("Comment \"{}\" already pushed on file {} ({})", issue.getRule(), issue.getPath(), issue.getLine());
        } else {
        
          // check if issue belongs to the Stash diff view
          String type = diffReport.getType(issue.getPath(), issue.getLine());
          if (type == null){
            LOGGER.info("Comment \"{}\" cannot be pushed to Stash like it does not belong to diff view - {} (line: {})", issue.getRule(), issue.getPath(), issue.getLine());
          } else{
          
            long line = diffReport.getLine(issue.getPath(), issue.getLine());
            
            stashClient.postCommentLineOnPullRequest(project,
                                                     repository,
                                                     pullRequestId,
                                                     MarkdownPrinter.printIssueMarkdown(issue, sonarQubeURL),
                                                     issue.getPath(),
                                                     line,
                                                     type);
  
            LOGGER.debug("Comment \"{}\" has been created ({}) on file {} ({})", issue.getRule(), type, issue.getPath(), line);
          }
        }
      }
      
      LOGGER.info("New SonarQube issues have been reported to Stash.");
      
    } catch (StashClientException e){
      LOGGER.error("Unable to link SonarQube issues to Stash: {}", e.getMessage());
      LOGGER.debug("Exception stack trace", e);
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
    if (result == null){
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
    if (result == null){
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
    if (result == null){
      throw new StashConfigurationException("Unable to get " + StashPlugin.STASH_PULL_REQUEST_ID + ": value is null");
    }
    
    return result;
  }
  
  
  
}
