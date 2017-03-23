package org.sonar.plugins.stash.issue.collector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;


public class SonarQubeCollectorTest {
  @Mock
  ProjectIssues projectIssues = mock(ProjectIssues.class);
  
  @Mock
  Issue issue1;
  
  @Mock
  Issue issue2;

  @Mock
  SensorContext context;
  
  @Mock
  Resource resource1;
  
  @Mock
  Resource resource2;
    
  @Mock
  Measure<Integer> measure1;
  
  @Mock
  Measure<Integer> measure2;
  
  @Mock
  Measure<Integer> measure3;
  
  @Mock
  Measure<Integer> measure4;
  
  @Mock
  InputFile inputFile1;
  
  @Mock
  InputFile inputFile2;

  DummyIssuePathResolver issuePathResolver;

  @Before
  public void setUp() throws Exception {
    
    ///////// File system objects /////////
    
    context = mock(SensorContext.class);

    ArrayList<InputFile> inputFiles = new ArrayList<InputFile>();
    inputFiles.add(inputFile1);
    inputFiles.add(inputFile2);
    
    FilePredicates filePredicates = mock(FilePredicates.class);
    when(filePredicates.all()).thenReturn(mock(FilePredicate.class));
    
    FileSystem fileSystem = mock(FileSystem.class);
    when(fileSystem.inputFiles((FilePredicate) anyObject())).thenReturn(inputFiles);
    when(fileSystem.predicates()).thenReturn(filePredicates);

    ///////// Metric object ////////
    
    measure1 = mock(Measure.class); 
    when(measure1.getValue()).thenReturn(33.33);
    
    measure2 = mock(Measure.class); 
    when(measure2.getValue()).thenReturn(100.0);
    
    measure3 = mock(Measure.class); 
    when(measure3.getValue()).thenReturn(66.66);
    
    measure4 = mock(Measure.class); 
    when(measure4.getValue()).thenReturn(100.0);
    
    resource1 = mock(Resource.class);
    when(context.getResource(inputFile1)).thenReturn(resource1);
    when(context.getMeasure(resource1, CoreMetrics.UNCOVERED_LINES)).thenReturn(measure1);
    when(context.getMeasure(resource1, CoreMetrics.LINES_TO_COVER)).thenReturn(measure2);
    
    resource2 = mock(Resource.class);
    when(context.getResource(inputFile2)).thenReturn(resource2);
    when(context.getMeasure(resource2, CoreMetrics.UNCOVERED_LINES)).thenReturn(measure3);
    when(context.getMeasure(resource2, CoreMetrics.LINES_TO_COVER)).thenReturn(measure4);
    
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

    issuePathResolver = new DummyIssuePathResolver();

    inputFile1 = new DefaultInputFile("project/path1").setKey("inputFile1");
    inputFile2 = new DefaultInputFile("project/path2").setKey("inputFile2");

    issuePathResolver.add(issue1, inputFile1.relativePath());
    issuePathResolver.add(issue2, inputFile2.relativePath());
  }
  
  @Test
  public void testExtractEmptyIssueReport() {
    ArrayList<Issue> issues = new ArrayList<Issue>();
    when(projectIssues.issues()).thenReturn(issues);
    
    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver);
    assertEquals(0, report.size());
  }
  
  @Test
  public void testExtractIssueReport() {
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);
    
    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver);
    assertEquals(2, report.size());
    assertEquals(1, countIssuesBySeverity(report, "severity1"));
    assertEquals(1, countIssuesBySeverity(report, "severity2"));

    Issue sqIssue1 = report.get(0);
    assertEquals("message1", sqIssue1.message());
    assertEquals("project/path1", issuePathResolver.getIssuePath(sqIssue1));
    assertEquals((Integer) 1, sqIssue1.line());

    Issue sqIssue2 = report.get(1);
    assertEquals("message2", sqIssue2.message());
    assertEquals("project/path2", issuePathResolver.getIssuePath(sqIssue2));
    assertEquals((Integer) 2, sqIssue2.line());

  }
  
  @Test
  public void testExtractIssueReportWithNoLine() {
    when(issue1.line()).thenReturn(null);  
    
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    when(projectIssues.issues()).thenReturn(issues);
    
    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver);
    assertEquals(1, report.size());
    assertEquals(1, countIssuesBySeverity(report, "severity1"));
    assertEquals(0, countIssuesBySeverity(report, "severity2"));

    Issue sqIssue1 = report.get(0);
    assertEquals("message1", sqIssue1.message());
    assertEquals("project/path1", issuePathResolver.getIssuePath(sqIssue1));
    assertEquals(null, sqIssue1.line());
  }
  
  @Test
  public void testExtractIssueReportWithOldOption() {
    when(issue1.isNew()).thenReturn(false);
    when(issue2.isNew()).thenReturn(true);
    
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);
    
    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver);
    assertEquals(1, report.size());
    assertEquals(0, countIssuesBySeverity(report, "severity1"));
    assertEquals(1, countIssuesBySeverity(report, "severity2"));
  }
  
  @Test
  public void testExtractIssueReportWithOneIssueWithoutInputFile(){
    issuePathResolver.clear();
    issuePathResolver.add(issue2, "project/path2");

    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);
    
    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver);
    assertEquals(1, report.size());
    assertEquals(0, countIssuesBySeverity(report, "severity1"));
    assertEquals(1, countIssuesBySeverity(report, "severity2"));

    Issue sqIssue2 = report.get(0);
    assertEquals("message2", sqIssue2.message());
    assertEquals("project/path2", issuePathResolver.getIssuePath(sqIssue2));
    assertEquals((Integer) 2, sqIssue2.line());
  }
}
