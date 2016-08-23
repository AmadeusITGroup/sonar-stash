package org.sonar.plugins.stash.issue.collector;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.stash.InputFileCache;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;

public final class SonarQubeCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SonarQubeCollector.class);
  
  private SonarQubeCollector() {
    // NOTHING TO DO
    // Pure static class
  }

  /**
   * Create issue report according to issue list generated during SonarQube
   * analysis.
   */
  public static SonarQubeIssuesReport extractIssueReport(ProjectIssues projectIssues, InputFileCache inputFileCache, File projectBaseDir, boolean includeExistingIssues) {
    SonarQubeIssuesReport result = new SonarQubeIssuesReport();

    for (Issue issue : projectIssues.issues()) {
      if (! issue.isNew() && !includeExistingIssues){
        LOGGER.debug("Issue {} is not a new issue and so, not added to the report", issue.key());
      } else {
        String key = issue.key();
        String severity = issue.severity();
        String rule = issue.ruleKey().toString();
        String message = issue.message();
  
        int line = 0;
        if (issue.line() != null) {
          line = issue.line();
        }
  
        InputFile inputFile = inputFileCache.getInputFile(issue.componentKey());
        if (inputFile == null){
          LOGGER.debug("Issue {} is not linked to a file, not added to the report", issue.key());
        } else {
          String path = new PathResolver().relativePath(projectBaseDir, inputFile.file());
             
          // Create the issue and Add to report
          SonarQubeIssue stashIssue = new SonarQubeIssue(key, severity, message, rule, path, line);
          result.add(stashIssue);
        } 
      }
    }

    return result;
  }
}
