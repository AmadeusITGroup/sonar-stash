package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Report {

  protected final List<Issue> issues;

  public Report() {
    this.issues = new ArrayList<>();
  }
  
  public void add(Issue issue) {
    issues.add(issue);
  }

  public List<Issue> getIssues(){
    return issues;
  }

  public int countIssues() {
    return issues.size();
  }
  
  public List<Issue> getIssuesBySeverity(String severity) {
    List<Issue> result = new ArrayList<>();
    for (Issue issue : issues) {
      if (StringUtils.equals(severity, issue.getSeverity())) {
        result.add(issue);
      }
    }
    
    return result;
  }

  public int countIssues(String severity) {
    return getIssuesBySeverity(severity).size();
  }
}
