package org.sonar.plugins.stash.issue.collector;

import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;
import static org.sonar.plugins.stash.StashPluginUtils.getIssuesBySeverity;


@RunWith(MockitoJUnitRunner.class)
public class SonarQubeCollectorTest {
  @Mock
  ProjectIssues projectIssues;

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

  Set<RuleKey> excludedRules;

  @Before
  public void setUp() throws Exception {

    // Activating debug level for class we are testing to ensure we go through all lines (because of squid:S2629)
    //    huge thanks to C. Loiseau for nailing the proper syntax to subdue the Logger !
    java.util.logging.Logger.getLogger(SonarQubeCollector.class.getCanonicalName())
                            .setLevel(java.util.logging.Level.ALL);


    ///////// File system objects /////////

    ArrayList<InputFile> inputFiles = new ArrayList<InputFile>();
    inputFiles.add(inputFile1);
    inputFiles.add(inputFile2);

    FilePredicates filePredicates = mock(FilePredicates.class);
    when(filePredicates.all()).thenReturn(mock(FilePredicate.class));

    FileSystem fileSystem = mock(FileSystem.class);
    when(fileSystem.inputFiles((FilePredicate)anyObject())).thenReturn(inputFiles);
    when(fileSystem.predicates()).thenReturn(filePredicates);

    ///////// Metric object ////////

    when(measure1.getValue()).thenReturn(33.33);

    when(measure2.getValue()).thenReturn(100.0);

    when(measure3.getValue()).thenReturn(66.66);

    when(measure4.getValue()).thenReturn(100.0);

    when(context.getResource(inputFile1)).thenReturn(resource1);
    when(context.getMeasure(resource1, CoreMetrics.UNCOVERED_LINES)).thenReturn(measure1);
    when(context.getMeasure(resource1, CoreMetrics.LINES_TO_COVER)).thenReturn(measure2);

    when(context.getResource(inputFile2)).thenReturn(resource2);
    when(context.getMeasure(resource2, CoreMetrics.UNCOVERED_LINES)).thenReturn(measure3);
    when(context.getMeasure(resource2, CoreMetrics.LINES_TO_COVER)).thenReturn(measure4);

    ///////// SonarQube issues /////////

    when(issue1.line()).thenReturn(1);
    when(issue1.message()).thenReturn("message1");
    when(issue1.key()).thenReturn("key1");
    when(issue1.severity()).thenReturn("severity1");
    when(issue1.componentKey()).thenReturn("component1");
    when(issue1.isNew()).thenReturn(true);

    RuleKey rule1 = mock(RuleKey.class);
    when(rule1.toString()).thenReturn("rule1");
    when(issue1.ruleKey()).thenReturn(rule1);

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

    excludedRules = new HashSet<>();
  }

  @Test
  public void testExtractEmptyIssueReport() {
    ArrayList<Issue> issues = new ArrayList<Issue>();
    when(projectIssues.issues()).thenReturn(issues);

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, false, excludedRules);
    assertEquals(0, report.size());
  }

  @Test
  public void testExtractIssueReport() {
    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, false, excludedRules);
    assertEquals(2, report.size());
    assertEquals(1, countIssuesBySeverity(report, "severity1"));
    assertEquals(1, countIssuesBySeverity(report, "severity2"));

    Issue sqIssue1 = report.get(0);
    assertEquals("message1", sqIssue1.message());
    assertEquals("project/path1", issuePathResolver.getIssuePath(sqIssue1));
    assertEquals((Integer)1, sqIssue1.line());

    Issue sqIssue2 = report.get(1);
    assertEquals("message2", sqIssue2.message());
    assertEquals("project/path2", issuePathResolver.getIssuePath(sqIssue2));
    assertEquals((Integer)2, sqIssue2.line());

  }

  @Test
  public void testExtractIssueReportWithNoLine() {
    when(issue1.line()).thenReturn(null);

    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    when(projectIssues.issues()).thenReturn(issues);

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, false, excludedRules);
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

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, false, excludedRules);
    assertEquals(1, report.size());
    assertEquals(0, countIssuesBySeverity(report, "severity1"));
    assertEquals(1, countIssuesBySeverity(report, "severity2"));
  }

  @Test
  public void testExtractIssueReportWithOneIssueWithoutInputFile() {
    issuePathResolver.clear();
    issuePathResolver.add(issue2, "project/path2");

    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, false, excludedRules);
    assertEquals(1, report.size());
    assertEquals(0, countIssuesBySeverity(report, "severity1"));
    assertEquals(1, countIssuesBySeverity(report, "severity2"));

    Issue sqIssue2 = report.get(0);
    assertEquals("message2", sqIssue2.message());
    assertEquals("project/path2", issuePathResolver.getIssuePath(sqIssue2));
    assertEquals((Integer)2, sqIssue2.line());
  }

  @Test
  public void testConstructorIsPrivate() throws Exception {

    // Let's use this for the greater good: we make sure that nobody can create an instance of this class
    Constructor constructor = SonarQubeCollector.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));

    // This part is for code coverage only (but is re-using the elments above... -_^)
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void testExtractIssueReportWithIncludeExistingIssuesOption() {
    when(issue1.isNew()).thenReturn(false);
    when(issue2.isNew()).thenReturn(true);

    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, true, excludedRules);
    assertEquals(2, report.size());
    assertEquals(1, getIssuesBySeverity(report, "severity1").size());
    assertEquals(1, getIssuesBySeverity(report, "severity2").size());
  }

  @Test
  public void testExtractIssueReportWithExcludedRules() {
    when(issue1.ruleKey()).thenReturn(RuleKey.of("foo", "bar"));
    when(issue2.ruleKey()).thenReturn(RuleKey.of("foo", "baz"));

    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);

    excludedRules.add(RuleKey.of("foo", "bar"));

    List<Issue> report = SonarQubeCollector.extractIssueReport(projectIssues, issuePathResolver, true, excludedRules);
    assertEquals(1, report.size());
    assertEquals("key2", report.get(0).key());
  }
}
