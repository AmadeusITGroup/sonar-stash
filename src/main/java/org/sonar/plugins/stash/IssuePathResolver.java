package org.sonar.plugins.stash;

import org.sonar.api.batch.postjob.issue.PostJobIssue;

@FunctionalInterface
public interface IssuePathResolver {
  String getIssuePath(PostJobIssue issue);
}
