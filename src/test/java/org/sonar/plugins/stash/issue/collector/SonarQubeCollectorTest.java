package org.sonar.plugins.stash.issue.collector;

import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.DefaultIssue;
import org.sonar.plugins.stash.IssuePathResolver;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

@RunWith(MockitoJUnitRunner.class)
public class SonarQubeCollectorTest {

  @Mock
  DefaultIssue issue1;

  @Mock
  DefaultIssue issue2;

  @Mock
  InputFile inputFile1;

  @Mock
  InputFile inputFile2;

  Set<RuleKey> excludedRules;

  IssuePathResolver ipr = new DummyIssuePathResolver();

  @Before
  public void setUp() throws Exception {

    ///////// SonarQube issues /////////

    when(issue1.line()).thenReturn(1);
    when(issue1.message()).thenReturn("message1");
    when(issue1.key()).thenReturn("key1");
    when(issue1.severity()).thenReturn(Severity.BLOCKER);
    when(issue1.componentKey()).thenReturn("module1:component1");
    when(issue1.isNew()).thenReturn(true);
    when(issue1.inputComponent()).thenReturn(new DefaultInputFile("module1", "file1"));

    RuleKey rule1 = mock(RuleKey.class);
    when(rule1.toString()).thenReturn("rule1");
    when(issue1.ruleKey()).thenReturn(rule1);

    when(issue2.line()).thenReturn(2);
    when(issue2.message()).thenReturn("message2");
    when(issue2.key()).thenReturn("key2");
    when(issue2.severity()).thenReturn(Severity.CRITICAL);
    when(issue2.componentKey()).thenReturn("module2:component2");
    when(issue2.isNew()).thenReturn(true);
    when(issue2.inputComponent()).thenReturn(new DefaultInputFile("module2", "file2"));

    RuleKey rule2 = mock(RuleKey.class);
    when(rule2.toString()).thenReturn("rule2");
    when(issue2.ruleKey()).thenReturn(rule2);

    inputFile1 = new DefaultInputFile("module1", "project/path1");
    inputFile2 = new DefaultInputFile("module2", "project/path2");
    when(issue1.inputComponent()).thenReturn(inputFile1);
    when(issue2.inputComponent()).thenReturn(inputFile2);

    excludedRules = new HashSet<>();
  }

  @Test
  public void testExtractEmptyIssueReport() {
    ArrayList<PostJobIssue> issues = new ArrayList<>();

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, false, excludedRules);
    assertEquals(0, report.size());
  }

  @Test
  public void testExtractIssueReport() {
    ArrayList<PostJobIssue> issues = new ArrayList<>();
    issues.add(issue1);
    issues.add(issue2);

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, false, excludedRules);
    assertEquals(2, report.size());
    assertEquals(1, countIssuesBySeverity(report, Severity.BLOCKER));
    assertEquals(1, countIssuesBySeverity(report, Severity.CRITICAL));

    PostJobIssue sqIssue1 = report.get(0);
    assertEquals("message1", sqIssue1.message());
    assertEquals("project/path1", ipr.getIssuePath(sqIssue1));
    assertEquals((Integer) 1, sqIssue1.line());

    PostJobIssue sqIssue2 = report.get(1);
    assertEquals("message2", sqIssue2.message());
    assertEquals("project/path2", ipr.getIssuePath(sqIssue2));
    assertEquals((Integer) 2, sqIssue2.line());

  }

  @Test
  public void testExtractIssueReportWithNoLine() {
    when(issue1.line()).thenReturn(null);

    ArrayList<PostJobIssue> issues = new ArrayList<>();
    issues.add(issue1);

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, false, excludedRules);
    assertEquals(1, report.size());
    assertEquals(1, countIssuesBySeverity(report, Severity.BLOCKER));
    assertEquals(0, countIssuesBySeverity(report, Severity.CRITICAL));

    PostJobIssue sqIssue1 = report.get(0);
    assertEquals("message1", sqIssue1.message());
    assertEquals("project/path1", ipr.getIssuePath(sqIssue1));
    assertEquals(null, sqIssue1.line());
  }

  @Test
  public void testExtractIssueReportWithOldOption() {
    when(issue1.isNew()).thenReturn(false);
    when(issue2.isNew()).thenReturn(true);

    ArrayList<PostJobIssue> issues = new ArrayList<>();
    issues.add(issue1);
    issues.add(issue2);

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, false, excludedRules);
    assertEquals(1, report.size());
    assertEquals(0, countIssuesBySeverity(report, Severity.BLOCKER));
    assertEquals(1, countIssuesBySeverity(report, Severity.CRITICAL));
  }

  @Test
  public void testExtractIssueReportWithOneIssueWithoutInputFile() {
    when(issue1.inputComponent()).thenReturn(null);

    ArrayList<PostJobIssue> issues = new ArrayList<>();
    issues.add(issue1);
    issues.add(issue2);

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, false, excludedRules);
    assertEquals(1, report.size());
    assertEquals(0, countIssuesBySeverity(report, Severity.BLOCKER));
    assertEquals(1, countIssuesBySeverity(report, Severity.CRITICAL));

    PostJobIssue sqIssue2 = report.get(0);
    assertEquals("message2", sqIssue2.message());
    assertEquals("project/path2", ipr.getIssuePath(sqIssue2));
    assertEquals((Integer) 2, sqIssue2.line());
  }

  @Test
  public void testExtractIssueReportWithIncludeExistingIssuesOption() {
    when(issue1.isNew()).thenReturn(false);
    when(issue2.isNew()).thenReturn(true);

    ArrayList<PostJobIssue> issues = new ArrayList<>();
    issues.add(issue1);
    issues.add(issue2);

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, true, excludedRules);
    assertEquals(2, report.size());
  }

  @Test
  public void testExtractIssueReportWithExcludedRules() {
    when(issue1.ruleKey()).thenReturn(RuleKey.of("foo", "bar"));
    when(issue2.ruleKey()).thenReturn(RuleKey.of("foo", "baz"));

    ArrayList<PostJobIssue> issues = new ArrayList<>();
    issues.add(issue1);
    issues.add(issue2);

    excludedRules.add(RuleKey.of("foo", "bar"));

    List<PostJobIssue> report = SonarQubeCollector.extractIssueReport(issues, ipr, true, excludedRules);
    assertEquals(1, report.size());
    assertEquals("key2", report.get(0).key());
  }

  @Test
  public void testShouldIncludeIssue() {
    Set<RuleKey> er = new HashSet<>();
    InputComponent ic = new DefaultInputFile("module", "some/path");

    assertFalse(
        SonarQubeCollector.shouldIncludeIssue(
            new DefaultIssue().setNew(false).setInputComponent(ic), ipr, false, er
        )
    );
    assertTrue(
        SonarQubeCollector.shouldIncludeIssue(
            new DefaultIssue().setNew(false).setInputComponent(ic), ipr, true, er
        )
    );
    assertTrue(
        SonarQubeCollector.shouldIncludeIssue(
            new DefaultIssue().setNew(true).setInputComponent(ic), ipr, false, er
        )
    );
    assertTrue(
        SonarQubeCollector.shouldIncludeIssue(
            new DefaultIssue().setNew(true).setInputComponent(ic), ipr, true, er
        )
    );
  }
}
