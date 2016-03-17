package org.sonar.plugins.stash.issue.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.InputFileCache;
import org.sonar.plugins.stash.InputFileCacheSensor;
import org.sonar.plugins.stash.client.SonarQubeClient;
import org.sonar.plugins.stash.exceptions.SonarQubeClientException;
import org.sonar.plugins.stash.exceptions.SonarQubeReportExtractionException;
import org.sonar.plugins.stash.issue.CoverageIssue;
import org.sonar.plugins.stash.issue.CoverageIssuesReport;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;


public class SonarQubeCollectorTest {
  
  File projectBaseDir;
  
  @Mock
  InputFileCache inputFileCache = mock(InputFileCache.class);
  
  @Mock
  ProjectIssues projectIssues = mock(ProjectIssues.class);
  
  @Mock
  Issue issue1;
  
  @Mock
  Issue issue2;
  
  @Mock
  InputFileCacheSensor inputFileCacheSensor;
  
  @Mock
  SensorContext context;
  
  @Mock
  Metric<Serializable> metric1;
  
  @Mock
  Metric<Serializable> metric2;
  
  @Mock
  Collection<Measure> measures1;
  
  @Mock
  Collection<Measure> measures2;
  
  @Mock
  InputFile inputFile1;
  
  @Mock
  InputFile inputFile2;
  
  @Mock
  SonarQubeClient sonarqubeClient;
  
  
  @Before
  public void setUp() throws Exception {
    
    ///////// File system objects /////////
    
    projectBaseDir = new File("baseDir");
    
    inputFile1 = mock(InputFile.class); 
    when(inputFile1.file()).thenReturn(new File("baseDir/project/path1"));
    when(inputFile1.relativePath()).thenReturn("baseDir/project/path1");
    
    inputFile2 = mock(InputFile.class); 
    when(inputFile2.file()).thenReturn(new File("baseDir/project/path2"));
    when(inputFile2.relativePath()).thenReturn("baseDir/project/path2");
    
    when(inputFileCache.getInputFile("component1")).thenReturn(inputFile1);
    when(inputFileCache.getInputFile("component2")).thenReturn(inputFile2);
    
    ArrayList<InputFile> inputFiles = new ArrayList<InputFile>();
    inputFiles.add(inputFile1);
    inputFiles.add(inputFile2);
    
    FilePredicates filePredicates = mock(FilePredicates.class);
    when(filePredicates.all()).thenReturn(mock(FilePredicate.class));
    
    FileSystem fileSystem = mock(FileSystem.class);
    when(fileSystem.inputFiles((FilePredicate) anyObject())).thenReturn(inputFiles);
    when(fileSystem.predicates()).thenReturn(filePredicates);
    
    inputFileCacheSensor = mock(InputFileCacheSensor.class);
    when(inputFileCacheSensor.getFileSystem()).thenReturn(fileSystem);
    
    
    ///////// Metric object ////////
    
    metric1 = mock(Metric.class);
    when(metric1.getName()).thenReturn(CoverageIssue.UNCOVERED_LINES_MEASURE_NAME);
    
    metric2 = mock(Metric.class);
    when(metric2.getName()).thenReturn(CoverageIssue.LINES_TO_COVER_MEASURE_NAME);
    
    Measure<Serializable> measure1 = mock(Measure.class); 
    when(measure1.getMetric()).thenReturn(metric1);
    when(measure1.getValue()).thenReturn(33.33);
    
    Measure<Serializable> measure2 = mock(Measure.class); 
    when(measure2.getMetric()).thenReturn(metric2);
    when(measure2.getValue()).thenReturn(100.0);
    
    Measure<Serializable> measure3 = mock(Measure.class); 
    when(measure3.getMetric()).thenReturn(metric1);
    when(measure3.getValue()).thenReturn(66.66);
    
    Measure<Serializable> measure4 = mock(Measure.class); 
    when(measure4.getMetric()).thenReturn(metric2);
    when(measure4.getValue()).thenReturn(100.0);
    
    measures1 = new ArrayList<Measure>();
    measures1.add(measure1);
    measures1.add(measure2);
    
    measures2 = new ArrayList<Measure>();
    measures2.add(measure3);
    measures2.add(measure4);
    
    context = mock(SensorContext.class);
    
    Resource resource1 = mock(Resource.class);
    when(context.getResource(inputFile1)).thenReturn(resource1);
    when(context.getMeasures(eq(resource1), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures1);
    
    Resource resource2 = mock(Resource.class);
    when(context.getResource(inputFile2)).thenReturn(resource2);
    when(context.getMeasures(eq(resource2), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures2);
    
    
    ///////// SonarQube Rest client /////////
    
    sonarqubeClient = mock(SonarQubeClient.class);
    when(sonarqubeClient.getCoveragePerFile("SonarQubeProject", "baseDir/project/path1")).thenReturn(70.0);
    when(sonarqubeClient.getCoveragePerFile("SonarQubeProject", "baseDir/project/path2")).thenReturn(60.0);
    when(sonarqubeClient.getCoveragePerProject("SonarQubeProject")).thenReturn(65.0);
    
    
    ///////// SonarQube issues /////////
    
    issue1 = mock(Issue.class);
    when(issue1.line()).thenReturn(1);
    when(issue1.message()).thenReturn("message1");
    when(issue1.key()).thenReturn("key1");
    when(issue1.severity()).thenReturn("severity1");
    when(issue1.componentKey()).thenReturn("component1");
    when(issue1.isNew()).thenReturn(true);
    
    RuleKey rule1 = mock(RuleKey.class);
    when(rule1.toString()).thenReturn("rule1");
    when(issue1.ruleKey()).thenReturn(rule1);
    
    issue2 = mock(Issue.class);
    when(issue2.line()).thenReturn(2);
    when(issue2.message()).thenReturn("message2");
    when(issue2.key()).thenReturn("key2");
    when(issue2.severity()).thenReturn("severity2");
    when(issue2.componentKey()).thenReturn("component2");
    when(issue2.isNew()).thenReturn(true);
    
    RuleKey rule2 = mock(RuleKey.class);
    when(rule2.toString()).thenReturn("rule2");
    when(issue2.ruleKey()).thenReturn(rule2);
  }
  
  @Test
  public void testExtractEmptyIssueReport() {
    ArrayList<Issue> issues = new ArrayList<Issue>();
    when(projectIssues.issues()).thenReturn(issues);
    
    SonarQubeIssuesReport report = SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
    assertTrue(report.countIssues() == 0);
  }
  
  @Test
  public void testExtractIssueReport() {
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);
    
    SonarQubeIssuesReport report = SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
    assertTrue(report.countIssues() == 2);
    assertTrue(report.countIssues("severity1") == 1);
    assertTrue(report.countIssues("severity2") == 1);
    
    SonarQubeIssue sqIssue1 = (SonarQubeIssue) report.getIssues().get(0);
    assertTrue(StringUtils.equals(sqIssue1.getMessage(), "message1"));
    assertTrue(StringUtils.equals(sqIssue1.getPath(), "project/path1"));
    assertTrue(sqIssue1.getLine() == 1);
    
    SonarQubeIssue sqIssue2 = (SonarQubeIssue) report.getIssues().get(1);
    assertTrue(StringUtils.equals(sqIssue2.getMessage(), "message2"));
    assertTrue(StringUtils.equals(sqIssue2.getPath(), "project/path2"));
    assertTrue(sqIssue2.getLine() == 2);
    
  }
  
  @Test
  public void testExtractIssueReportWithNoLine() {
    when(issue1.line()).thenReturn(null);  
    
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    when(projectIssues.issues()).thenReturn(issues);
    
    SonarQubeIssuesReport report = SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
    assertTrue(report.countIssues() == 1);
    assertTrue(report.countIssues("severity1") == 1);
    assertTrue(report.countIssues("severity2") == 0);
    
    SonarQubeIssue sqIssue = (SonarQubeIssue) report.getIssues().get(0);
    assertTrue(StringUtils.equals(sqIssue.getMessage(), "message1"));
    assertTrue(StringUtils.equals(sqIssue.getPath(), "project/path1"));
    assertTrue(sqIssue.getLine() == 0);
    
  }
  
  @Test
  public void testExtractIssueReportWithOldOption() {
    when(issue1.isNew()).thenReturn(false);
    when(issue2.isNew()).thenReturn(true);
    
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);
    
    SonarQubeIssuesReport report = SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
    assertTrue(report.countIssues() == 1);
    assertTrue(report.countIssues("severity1") == 0);
    assertTrue(report.countIssues("severity2") == 1);
  }
  
  @Test
  public void testExtractIssueReportWithOneIssueWithoutInputFile(){
    when(inputFileCache.getInputFile("component1")).thenReturn(null);
    
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);
    
    SonarQubeIssuesReport report = SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
    assertTrue(report.countIssues() == 1);
    assertTrue(report.countIssues("severity1") == 0);
    assertTrue(report.countIssues("severity2") == 1);
    
    SonarQubeIssue sqIssue = (SonarQubeIssue) report.getIssues().get(0);
    assertTrue(StringUtils.equals(sqIssue.getMessage(), "message2"));
    assertTrue(StringUtils.equals(sqIssue.getPath(), "project/path2"));
    assertTrue(sqIssue.getLine() == 2);
  }
  
  @Test
  public void testExtractCoverageReport() throws SonarQubeClientException {
    CoverageIssuesReport result = SonarQubeCollector.extractCoverageReport("SonarQubeProject", context, inputFileCacheSensor, "MAJOR", sonarqubeClient);
    
    assertEquals(2, result.countIssues());
    
    CoverageIssue coverageIssue1 = (CoverageIssue) result.getIssues().get(0);
    assertTrue("Previous expected code coverage: 70.0 but was " + coverageIssue1.getPreviousCoverage(), coverageIssue1.getPreviousCoverage() == 70.0);
    assertTrue("Expected code coverage: 66.7 but was " + coverageIssue1.getCoverage(), coverageIssue1.getCoverage() == 66.7);
    assertEquals("MAJOR", coverageIssue1.getSeverity());
    
    CoverageIssue coverageIssue2 = (CoverageIssue) result.getIssues().get(1);
    assertTrue("Previous expected code coverage: 60.0 but was " + coverageIssue2.getPreviousCoverage(), coverageIssue2.getPreviousCoverage() == 60.0);
    assertTrue("Expected code coverage: 33.3 but was " + coverageIssue2.getCoverage(), coverageIssue2.getCoverage() == 33.3);
    assertEquals("MAJOR", coverageIssue2.getSeverity());
    
    assertTrue("Previous expected project code coverage: 65.0 but was " + result.getPreviousProjectCoverage(), result.getPreviousProjectCoverage() == 65.0);
    assertTrue("Expected project code coverage: 50.0 but was " + result.getProjectCoverage(), result.getProjectCoverage() == 50.0);
  }
  
  @Test
  public void testExtractCoverageReportWithNoInputFiles() throws SonarQubeClientException {
    ArrayList<InputFile> inputFiles = new ArrayList<InputFile>();
    
    FilePredicates filePredicates = mock(FilePredicates.class);
    when(filePredicates.all()).thenReturn(mock(FilePredicate.class));
    
    FileSystem fileSystem = mock(FileSystem.class);
    when(fileSystem.inputFiles((FilePredicate) anyObject())).thenReturn(inputFiles);
    when(fileSystem.predicates()).thenReturn(filePredicates);
    
    inputFileCacheSensor = mock(InputFileCacheSensor.class);
    when(inputFileCacheSensor.getFileSystem()).thenReturn(fileSystem);
    
    CoverageIssuesReport result = SonarQubeCollector.extractCoverageReport("SonarQubeProject", context, inputFileCacheSensor, "MAJOR", sonarqubeClient);
    
    assertEquals(0, result.countIssues());
    assertTrue("Previous expected project code coverage: 65.0 but was " + result.getPreviousProjectCoverage(), result.getPreviousProjectCoverage() == 65.0);
    assertTrue("Expected project code coverage: 0.0 but was " + result.getProjectCoverage(), result.getProjectCoverage() == 0.0);
  }
  
  @Test
  public void testExtractCoverageReportWithNoUncoveredMeasure() throws SonarQubeClientException {
    Measure<Serializable> measure1 = mock(Measure.class); 
    when(measure1.getMetric()).thenReturn(metric2);
    when(measure1.getValue()).thenReturn(100.0);
    
    Measure<Serializable> measure2 = mock(Measure.class); 
    when(measure2.getMetric()).thenReturn(metric2);
    when(measure2.getValue()).thenReturn(100.0);
    
    ArrayList<Measure> measures1 = new ArrayList<Measure>();
    measures1.add(measure1);
    
    ArrayList<Measure> measures2 = new ArrayList<Measure>();
    measures2.add(measure2);
    
    SensorContext context = mock(SensorContext.class);
    
    Resource resource1 = mock(Resource.class);
    when(context.getResource(inputFile1)).thenReturn(resource1);
    when(context.getMeasures(eq(resource1), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures1);
    
    Resource resource2 = mock(Resource.class);
    when(context.getResource(inputFile2)).thenReturn(resource2);
    when(context.getMeasures(eq(resource2), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures2);  
    
    CoverageIssuesReport result = SonarQubeCollector.extractCoverageReport("SonarQubeProject", context, inputFileCacheSensor, "MAJOR", sonarqubeClient);
  
    assertEquals(0, result.countIssues());
    assertTrue("Previous expected project code coverage: 65.0 but was " + result.getPreviousProjectCoverage(), result.getPreviousProjectCoverage() == 65.0);
    assertTrue("Expected project code coverage: 0.0 but was " + result.getProjectCoverage(), result.getProjectCoverage() == 0.0);
  }
  
  @Test
  public void testExtractCoverageReportWithNoLinesToCoverMeasure() throws SonarQubeClientException {
    Measure<Serializable> measure1 = mock(Measure.class); 
    when(measure1.getMetric()).thenReturn(metric1);
    when(measure1.getValue()).thenReturn(40.0);
    
    Measure<Serializable> measure2 = mock(Measure.class); 
    when(measure2.getMetric()).thenReturn(metric1);
    when(measure2.getValue()).thenReturn(80.0);
    
    ArrayList<Measure> measures1 = new ArrayList<Measure>();
    measures1.add(measure1);
    
    ArrayList<Measure> measures2 = new ArrayList<Measure>();
    measures2.add(measure2);
    
    SensorContext context = mock(SensorContext.class);
    
    Resource resource1 = mock(Resource.class);
    when(context.getResource(inputFile1)).thenReturn(resource1);
    when(context.getMeasures(eq(resource1), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures1);
    
    Resource resource2 = mock(Resource.class);
    when(context.getResource(inputFile2)).thenReturn(resource2);
    when(context.getMeasures(eq(resource2), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures2);  
    
    CoverageIssuesReport result = SonarQubeCollector.extractCoverageReport("SonarQubeProject", context, inputFileCacheSensor, "MAJOR", sonarqubeClient);
  
    assertEquals(0, result.countIssues());
    assertTrue("Previous expected project code coverage: 65.0 but was " + result.getPreviousProjectCoverage(), result.getPreviousProjectCoverage() == 65.0);
    assertTrue("Expected project code coverage: 0.0 but was " + result.getProjectCoverage(), result.getProjectCoverage() == 0.0);
  }
  
  @Test
  public void testExtractCoverageReportWithNoMeasures() throws SonarQubeClientException {
    SensorContext context = mock(SensorContext.class);
    
    Resource resource1 = mock(Resource.class);
    when(context.getResource(inputFile1)).thenReturn(resource1);
    when(context.getMeasures(eq(resource1), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(null);
    
    Resource resource2 = mock(Resource.class);
    when(context.getResource(inputFile2)).thenReturn(resource2);
    when(context.getMeasures(eq(resource2), (MeasuresFilter<Collection<Measure>>) anyObject())).thenReturn(measures2);  
    
    CoverageIssuesReport result = SonarQubeCollector.extractCoverageReport("SonarQubeProject", context, inputFileCacheSensor, "MAJOR", sonarqubeClient);
  
    assertEquals(1, result.countIssues());
    
    CoverageIssue coverageIssue = (CoverageIssue) result.getIssues().get(0);
    assertTrue("Previous expected code coverage: 60.0 but was " + coverageIssue.getPreviousCoverage(), coverageIssue.getPreviousCoverage() == 60.0);
    assertTrue("Expected code coverage: 33.3 but was " + coverageIssue.getCoverage(), coverageIssue.getCoverage() == 33.3);
    assertEquals("MAJOR", coverageIssue.getSeverity());
    
    assertTrue("Previous expected project code coverage: 65.0 but was " + result.getPreviousProjectCoverage(), result.getPreviousProjectCoverage() == 65.0);
    assertTrue("Expected project code coverage: 33.3 but was " + result.getProjectCoverage(), result.getProjectCoverage() == 33.3);
  }
  
  @Test
  public void testExtractCoverageReportWithException() throws SonarQubeClientException {
    doThrow(new SonarQubeClientException("SonarQubeClientException for Test")).when(sonarqubeClient).getCoveragePerFile(anyString(), anyString());
    
    try {
      SonarQubeCollector.extractCoverageReport("SonarQubeProject", context, inputFileCacheSensor, "MAJOR", sonarqubeClient);
    
      assertFalse("extractCoverageReport should raise a SonarQubeClientException", true);
    
    } catch (SonarQubeClientException e) {
      assertTrue("extractCoverageReport has raised the expected SonarQubeClientException", true);
    }
  }
  
  @Test
  public void testExtractCoverage() throws SonarQubeReportExtractionException {
    String jsonBody = "[{\"msr\": [{\"key\": \"line_coverage\", \"val\": 10.12345}]}]";
    
    double coverage = SonarQubeCollector.extractCoverage(jsonBody);
    assertTrue("Expected coverage: 10.12345 but was " + coverage, coverage == 10.12345);
  }
  
  @Test
  public void testExtractCoverageWithNoline_coverage() throws SonarQubeReportExtractionException {
    String jsonBody = "[{\"msr\": [{\"key\": \"metric\", \"val\": 10.12345}]}]";
    
    double coverage = SonarQubeCollector.extractCoverage(jsonBody);
    assertTrue("Expected coverage: 0 but was " + coverage, coverage == 0);
  }
  
  @Test
  public void testExtractCoverageWithParseException() throws SonarQubeReportExtractionException {
    String jsonBody = "[{\"msr\": [{\"key\": \"metric\", \"val\": 10.12345]}]";
    
    try {
      SonarQubeCollector.extractCoverage(jsonBody);
      assertFalse("No ParseException has been raised", true);
    
    } catch (SonarQubeReportExtractionException e) {
      assertTrue("ParseException has been raised as expected", true);
    }
  }
}
