package org.sonar.plugins.stash.issue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class SonarQubeIssuesReport {
  
  private final List<SonarQubeIssue> issues;

  public SonarQubeIssuesReport() {
    this.issues = new ArrayList<>();
  }
  
  public void add(SonarQubeIssue issue) {
    issues.add(issue);
  }

  public List<SonarQubeIssue> getIssues(){
    return issues;
  }
  
  public List<SonarQubeIssue> getIssuesBySeverity(String severity) {
    List<SonarQubeIssue> result = new ArrayList<>();
    for (SonarQubeIssue issue : issues) {
      if (StringUtils.equals(severity, issue.getSeverity())) {
        result.add(issue);
      }
    }
    
    return result;
  }

  public int countIssues() {
    return issues.size();
  }

  public int countIssues(String severity) {
    return getIssuesBySeverity(severity).size();
  }

  /**
   * Extract rule list according to a severity.
   */
  public Map<String, SonarQubeIssue> getUniqueRulesBySeverity(String severity) {
    Map<String, SonarQubeIssue> result = new HashMap<>();

    for (SonarQubeIssue issue : getIssuesBySeverity(severity)) {
      result.put(issue.getRule(), issue);
    }
    
    return result;
  }
}
