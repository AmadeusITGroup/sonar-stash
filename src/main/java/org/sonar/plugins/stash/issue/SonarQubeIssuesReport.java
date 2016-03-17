package org.sonar.plugins.stash.issue;

import java.util.HashMap;
import java.util.Map;

public class SonarQubeIssuesReport extends Report {
  
  public SonarQubeIssuesReport() {
    super();
  }
  
  /**
   * Extract rule list according to a severity.
   */
  public Map<String, SonarQubeIssue> getUniqueRulesBySeverity(String severity) {
    Map<String, SonarQubeIssue> result = new HashMap<>();

    for (Issue issue : getIssuesBySeverity(severity)) {
      SonarQubeIssue sonarqubeIssue = (SonarQubeIssue) issue;
      result.put(sonarqubeIssue.getRule(), sonarqubeIssue);
    }
    
    return result;
  }
}
