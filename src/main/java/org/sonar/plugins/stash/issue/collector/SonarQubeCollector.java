package org.sonar.plugins.stash.issue.collector;

import java.util.Set;
import static org.sonar.plugins.stash.StashPluginUtils.isProjectWide;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.IssuePathResolver;

public final class SonarQubeCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SonarQubeCollector.class);

  private SonarQubeCollector() {
    // Hiding implicit public constructor with an explicit private one (squid:S1118)
  }

  /**
   * Create issue report according to issue list generated during SonarQube
   * analysis.
   */
  public static List<Issue> extractIssueReport(
      ProjectIssues projectIssues, IssuePathResolver issuePathResolver,
      boolean includeExistingIssues, Set<RuleKey> excludedRules, Project project) {
    return StreamSupport.stream(
        projectIssues.issues().spliterator(), false)
                        .filter(issue -> shouldIncludeIssue(
                            issue, issuePathResolver,
                            includeExistingIssues, excludedRules, project
                        ))
                        .collect(Collectors.toList());
  }

  private static boolean shouldIncludeIssue(
      Issue issue, IssuePathResolver issuePathResolver,
      boolean includeExistingIssues, Set<RuleKey> excludedRules,
      Project project
  ) {
    if (!includeExistingIssues && !issue.isNew()) {
      // squid:S2629 : no evaluation required if the logging level is not activated
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Issue {} is not a new issue and so, not added to the report", issue.key());
      }
      return false;
    }

    if (excludedRules.contains(issue.ruleKey())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Issue {} is ignored, not added to the report", issue.key());
      }
      return false;
    }

    String path = issuePathResolver.getIssuePath(issue);
    if (!isProjectWide(issue, project) && path == null) {
      // squid:S2629 : no evaluation required if the logging level is not activated
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Issue {} is not linked to a file, not added to the report", issue.key());
      }
      return false;
    }
    return true;
  }
}
