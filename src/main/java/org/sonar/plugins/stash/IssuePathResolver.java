package org.sonar.plugins.stash;

import org.sonar.api.issue.Issue;

@FunctionalInterface
public interface IssuePathResolver {
  // this field is automatically public and static by default (pmd:UnusedModifier)
  String getIssuePath(Issue issue);
}
