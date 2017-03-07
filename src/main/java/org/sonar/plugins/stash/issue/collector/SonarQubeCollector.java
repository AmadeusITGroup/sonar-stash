package org.sonar.plugins.stash.issue.collector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.plugins.stash.IssuePathResolver;

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
  public static List<Issue> extractIssueReport(ProjectIssues projectIssues, IssuePathResolver issuePathResolver, File projectBaseDir) {
    List<Issue> result = new ArrayList<>();

    for (Issue issue : projectIssues.issues()) {
      if (!issue.isNew()){
        LOGGER.debug("Issue {} is not a new issue and so, not added to the report", issue.key());
        continue;
      }

      String path = issuePathResolver.getIssuePath(issue);
      if (path == null) {
        LOGGER.debug("Issue {} is not linked to a file, not added to the report", issue.key());
        continue;
      }

      result.add(issue);
    }

    return result;
  }
}
