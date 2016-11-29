package org.sonar.plugins.stash.issue;

import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.sonar.plugins.stash.StashPluginUtils;


public class CoverageIssuesReport extends Report {
  
  private double previousProjectCoverage;
  
  public CoverageIssuesReport() {
    super();
    this.previousProjectCoverage = 0;
  }
  
  public double getPreviousProjectCoverage() {
    return previousProjectCoverage;
  }

  public void setPreviousProjectCoverage(double previousProjectCoverage) {
    this.previousProjectCoverage = previousProjectCoverage;
  }
  
  public boolean isEmpty() {
    return ((int)getProjectCoverage() == 0) && getIssues().isEmpty();
  }
  
  public Collection getLoweredIssues() {
    return CollectionUtils.select(issues, new Predicate() {
      @Override
      public boolean evaluate(Object object) {
        CoverageIssue coverageIssue = (CoverageIssue) object; 
        return coverageIssue.isLowered();
      }
    });
  }
  
  public int countLoweredIssues(final String severity) {
    return CollectionUtils.countMatches(getLoweredIssues(), new Predicate() {
      @Override
      public boolean evaluate(Object object) {
        CoverageIssue coverageIssue = (CoverageIssue) object; 
        return StringUtils.equals(coverageIssue.getSeverity(), severity);
      }
    });
  }
 
  public int countLoweredIssues() {
   return getLoweredIssues().size();
 }
  
  public double getProjectCoverage() {
    double result = 0;
    
    double sumLinesToCover = 0;
    double sumUncoveredLines = 0;
    
    for (Issue issue: issues) {
      sumLinesToCover   += ((CoverageIssue) issue).getLinesToCover();
      sumUncoveredLines += ((CoverageIssue) issue).getUncoveredLines();
    }
    
    if ((int)sumLinesToCover != 0) {
      result = StashPluginUtils.formatDouble((1 - (sumUncoveredLines / sumLinesToCover)) * 100);
    }
    
    return result;
  }
  
  public double getEvolution() {
    double diffProjectCoverage = this.getProjectCoverage() - this.getPreviousProjectCoverage();
    return StashPluginUtils.formatDouble(diffProjectCoverage);
  }
}
