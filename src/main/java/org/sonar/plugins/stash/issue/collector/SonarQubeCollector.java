package org.sonar.plugins.stash.issue.collector;

import java.util.Set;

import static org.sonar.plugins.stash.StashPluginUtils.isProjectWide;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.rule.RuleKey;
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
  public static List<PostJobIssue> extractIssueReport(
      Iterable<PostJobIssue> issues, IssuePathResolver issuePathResolver,
      boolean includeExistingIssues, Set<RuleKey> excludedRules) {
    return StreamSupport.stream(
        issues.spliterator(), false)
                        .filter(issue -> shouldIncludeIssue(
                            issue, issuePathResolver,
                            includeExistingIssues, excludedRules
                        ))
                        .collect(Collectors.toList());
  }

  static boolean shouldIncludeIssue(
      PostJobIssue issue, IssuePathResolver issuePathResolver,
      boolean includeExistingIssues, Set<RuleKey> excludedRules
  ) {
    if (!includeExistingIssues && !issue.isNew()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Issue {} is not a new issue and so, NOT ADDED to the report"
                + ", issue.componentKey = {}, issue.key = {}, issue.ruleKey = {}, issue.message = {}, issue.line = {}",
                issue, issue.componentKey(), issue.key(), issue.ruleKey(), issue.message(), issue.line());
      }
      return false;
    }

    if (excludedRules.contains(issue.ruleKey())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Issue {} is ignored, NOT ADDED to the report"
                + ", issue.componentKey = {}, issue.key = {}, issue.ruleKey = {}, issue.message = {}, issue.line = {}",
                issue, issue.componentKey(), issue.key(), issue.ruleKey(), issue.message(), issue.line());
      }
      return false;
    }

    String path = issuePathResolver.getIssuePath(issue);
    if (!isProjectWide(issue) && path == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Issue {} is not linked to a file, NOT ADDED to the report"
                + ", issue.componentKey = {}, issue.key = {}, issue.ruleKey = {}, issue.message = {}, issue.line = {}",
                issue, issue.componentKey(), issue.key(), issue.ruleKey(), issue.message(), issue.line());
      }
      return false;
    }
    
    LOGGER.debug("Issue {} is ADDED to the report"
            + ", issue.componentKey = {}, issue.key = {}, issue.ruleKey = {}, issue.message = {}, issue.line = {}",
            issue, issue.componentKey(), issue.key(), issue.ruleKey(), issue.message(), issue.line());
    return true;
  }
}
