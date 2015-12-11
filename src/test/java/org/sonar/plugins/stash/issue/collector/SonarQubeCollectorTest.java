package org.sonar.plugins.stash.issue.collector;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.InputFileCache;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  @Before
  public void setUp() {
    projectBaseDir = new File("baseDir");

    InputFile inputFile1 = mock(InputFile.class);
    when(inputFile1.file()).thenReturn(new File("baseDir/project/path1"));

    InputFile inputFile2 = mock(InputFile.class);
    when(inputFile2.file()).thenReturn(new File("baseDir/project/path2"));

    when(inputFileCache.getInputFile("component1")).thenReturn(inputFile1);
    when(inputFileCache.getInputFile("component2")).thenReturn(inputFile2);

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

    SonarQubeIssue sqIssue1 = report.getIssues().get(0);
    assertTrue(StringUtils.equals(sqIssue1.getMessage(), "message1"));
    assertTrue(StringUtils.equals(sqIssue1.getPath(), "project/path1"));
    assertTrue(sqIssue1.getLine() == 1);

    SonarQubeIssue sqIssue2 = report.getIssues().get(1);
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

    SonarQubeIssue sqIssue = report.getIssues().get(0);
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
  public void testExtractIssueReportWithOneIssueWithoutInputFile() {
    when(inputFileCache.getInputFile("component1")).thenReturn(null);

    ArrayList<Issue> issues = new ArrayList<Issue>();
    issues.add(issue1);
    issues.add(issue2);
    when(projectIssues.issues()).thenReturn(issues);

    SonarQubeIssuesReport report = SonarQubeCollector.extractIssueReport(projectIssues, inputFileCache, projectBaseDir);
    assertTrue(report.countIssues() == 1);
    assertTrue(report.countIssues("severity1") == 0);
    assertTrue(report.countIssues("severity2") == 1);

    SonarQubeIssue sqIssue = report.getIssues().get(0);
    assertTrue(StringUtils.equals(sqIssue.getMessage(), "message2"));
    assertTrue(StringUtils.equals(sqIssue.getPath(), "project/path2"));
    assertTrue(sqIssue.getLine() == 2);
  }
}
