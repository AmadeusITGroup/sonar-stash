package org.sonar.plugins.stash.issue;

import org.sonar.plugins.stash.StashPluginUtils;

public class CoverageIssue extends Issue {

  public static final String CODE_COVERAGE_ISSUE_KEY = "sonar.coverage.lowered";
 
  private double uncoveredLines;
  private double linesToCover;
  
  private double previousCoverage;
  
  public CoverageIssue(String severity, String path) {
    super(CODE_COVERAGE_ISSUE_KEY, severity, path, 0);
  }

  public double getUncoveredLines() {
    return uncoveredLines;
  }

  public void setUncoveredLines(double uncoveredLines) {
    this.uncoveredLines = uncoveredLines;
  }

  public double getLinesToCover() {
    return linesToCover;
  }

  public void setLinesToCover(double linesToCover) {
    this.linesToCover = linesToCover;
  }
  
  public double getCoverage() {
    double result = 0;
    if ((int)linesToCover != 0) {
      result = StashPluginUtils.formatDouble((1 - (uncoveredLines / linesToCover)) * 100);
    }
    
    return result;
  }
  
  public double getPreviousCoverage() {
    return previousCoverage;
  }

  public void setPreviousCoverage(double previousCoverage) {
    this.previousCoverage = previousCoverage;
  }

  @Override
  public String getMessage() {
   return "Code coverage of file " + path + " lowered from " + previousCoverage + "% to " + getCoverage() + "%.";
  }
  
  public boolean isLowered() {
     return (previousCoverage - getCoverage()) > 0;
  }

  @Override
  public String printIssueMarkdown(String sonarQubeURL) {
    StringBuilder sb = new StringBuilder();
    sb.append(MarkdownPrinter.printSeverityMarkdown(severity)).append(getMessage());

    return sb.toString();
  }

}
