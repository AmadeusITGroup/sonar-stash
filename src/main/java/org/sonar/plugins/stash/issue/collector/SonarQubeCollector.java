package org.sonar.plugins.stash.issue.collector;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.plugins.stash.InputFileCache;
import org.sonar.plugins.stash.InputFileCacheSensor;
import org.sonar.plugins.stash.client.SonarQubeClient;
import org.sonar.plugins.stash.exceptions.SonarQubeClientException;
import org.sonar.plugins.stash.exceptions.SonarQubeReportExtractionException;
import org.sonar.plugins.stash.issue.CoverageIssue;
import org.sonar.plugins.stash.issue.CoverageIssuesReport;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;

public final class SonarQubeCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SonarQubeCollector.class);
  
  private SonarQubeCollector() {
    // NOTHING TO DO
    // Pure static class
  }

  /**
   * Create issue report according to issue list generated during SonarQube
   * analysis.
   */
  public static SonarQubeIssuesReport extractIssueReport(ProjectIssues projectIssues, InputFileCache inputFileCache, File projectBaseDir) {
    SonarQubeIssuesReport result = new SonarQubeIssuesReport();

    for (Issue issue : projectIssues.issues()) {
      if (! issue.isNew()){
        LOGGER.debug("Issue {} is not a new issue and so, not added to the report", issue.key());
      } else {
        String key = issue.key();
        String severity = issue.severity();
        String rule = issue.ruleKey().toString();
        String message = issue.message();
  
        int line = 0;
        if (issue.line() != null) {
          line = issue.line();
        }
  
        InputFile inputFile = inputFileCache.getInputFile(issue.componentKey());
        if (inputFile == null){
          LOGGER.debug("Issue {} is not linked to a file, not added to the report", issue.key());
        } else {
          String path = new PathResolver().relativePath(projectBaseDir, inputFile.file());
             
          // Create the issue and Add to report
          SonarQubeIssue stashIssue = new SonarQubeIssue(key, severity, message, rule, path, line);
          result.add(stashIssue);
        } 
      }
    }

    return result;
  }
  
  /**
   * Extract Code Coverage report to be published into the pull-request.
   * @throws SonarQubeClientException 
   */
  public static CoverageIssuesReport extractCoverageReport(String sonarQubeProjectKey, SensorContext context,
      InputFileCacheSensor inputFileCacheSensor, String codeCoverageSeverity, SonarQubeClient sonarqubeClient) throws SonarQubeClientException {
    
    CoverageIssuesReport result = new CoverageIssuesReport();
    
    FileSystem fileSystem = inputFileCacheSensor.getFileSystem();
    for (org.sonar.api.batch.fs.InputFile f : fileSystem.inputFiles(fileSystem.predicates().all())) {
      
      Double linesToCover = null;
      Double uncoveredLines = null;

      Measure<Integer> linesToCoverMeasure = context.getMeasure(context.getResource(f), CoreMetrics.LINES_TO_COVER);
      if (linesToCoverMeasure != null) {
        linesToCover = linesToCoverMeasure.getValue();
      }
      
      Measure<Integer> uncoveredLinesMeasure = context.getMeasure(context.getResource(f), CoreMetrics.UNCOVERED_LINES);
      if (uncoveredLinesMeasure != null) {
        uncoveredLines = uncoveredLinesMeasure.getValue();
      }
      
      // get lines_to_cover, uncovered_lines
      if ((linesToCover != null) && (uncoveredLines != null)) {
        double previousCoverage = sonarqubeClient.getCoveragePerFile(sonarQubeProjectKey, f.relativePath());
  
        CoverageIssue issue = new CoverageIssue(codeCoverageSeverity, f.relativePath());
        issue.setLinesToCover(linesToCover);
        issue.setUncoveredLines(uncoveredLines);
        issue.setPreviousCoverage(previousCoverage);
  
        result.add(issue);
        LOGGER.debug(issue.getMessage());
      }
    }
  
    // set previous project coverage from SonarQube server
    double previousProjectCoverage = sonarqubeClient.getCoveragePerProject(sonarQubeProjectKey);
    result.setPreviousProjectCoverage(previousProjectCoverage);

    return result;
  }
  
  
  /**
   * Extract Code Coverage retrieve by the SonarQube analysis.
   */
  public static double extractCoverage(String jsonBody) throws SonarQubeReportExtractionException {
    double result = 0;
    
    try {
      JSONArray jsonFiles = (JSONArray) new JSONParser().parse(jsonBody);
      if (jsonFiles != null) {
        return result;
      }

      for (Object objectFile : jsonFiles.toArray()) {

        JSONObject jsonFile = (JSONObject) objectFile;
        JSONArray jsonMeasures = (JSONArray) jsonFile.get("msr");
        
        if (jsonMeasures == null) {
          continue; // Let's have a look at the next jsonFiles out of the list found
        }

        result = getCoverageFromMeasures(jsonMeasures);
      }
    } catch (ParseException e) {
      throw new SonarQubeReportExtractionException(e);
    }
    
    return result;
  }


  /*
  * Extract Coverage data from Measures section found in the coverage report
  */
  private static double getCoverageFromMeasures(JSONArray jsonMeasures) throws SonarQubeReportExtractionException {
    double result = 0;

    for (Object obj : jsonMeasures.toArray()) {
      JSONObject jsonMsr = (JSONObject) obj;

      String key = (String) jsonMsr.get("key");

      if (StringUtils.equals(key, "line_coverage")) {
        result = (Double) jsonMsr.get("val");
        break;
      }
    }

    return result;
  }
}
