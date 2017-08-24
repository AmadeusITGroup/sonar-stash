package org.sonar.plugins.stash.fixtures;

import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.plugins.stash.IssuePathResolver;

import java.util.HashMap;
import java.util.Map;

public class DummyIssuePathResolver implements IssuePathResolver {
  private Map<String, String> db = new HashMap<>();

  public void add(PostJobIssue issue, String path) {
    db.put(issue.key(), path);
  }

  @Override
  public String getIssuePath(PostJobIssue issue) {
    return db.get(issue.key());
  }

  public void clear() {
    db.clear();
  }
}
