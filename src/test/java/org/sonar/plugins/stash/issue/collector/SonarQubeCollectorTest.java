package org.sonar.plugins.stash.issue.collector;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.plugins.stash.StashPluginUtils.countIssuesBySeverity;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.DefaultIssue;
import org.sonar.plugins.stash.IssuePathResolver;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

  @BeforeEach
  public void setUp() throws Exception {

    ///////// SonarQube issues /////////
    Path baseDir = Paths.get("./src/test/java");
    when(issue1.line()).thenReturn(1);
    when(issue1.message()).thenReturn("message1");
    when(issue1.key()).thenReturn("key1");
    when(issue1.severity()).thenReturn(Severity.BLOCKER);
    when(issue1.componentKey()).thenReturn("module1:component1");
    when(issue1.isNew()).thenReturn(true);
    when(issue1.inputComponent()).thenReturn(new DefaultInputFile(new DefaultIndexedFile("module1", baseDir, "file1", "de_DE"), x -> x.hash()));
    RuleKey rule1 = mock(RuleKey.class);
    when(rule1.toString()).thenReturn("rule1");
    when(issue1.ruleKey()).thenReturn(rule1);

    when(issue2.line()).thenReturn(2);
    when(issue2.message()).thenReturn("message2");
    when(issue2.key()).thenReturn("key2");
    when(issue2.severity()).thenReturn(Severity.CRITICAL);
    when(issue2.componentKey()).thenReturn("module2:component2");
    when(issue2.isNew()).thenReturn(true);
    when(issue2.inputComponent()).thenReturn(new DefaultInputFile(new DefaultIndexedFile("module2", baseDir, "file2", "de_DE"), x -> x.hash()));

    RuleKey rule2 = mock(RuleKey.class);
    when(rule2.toString()).thenReturn("rule2");
    when(issue2.ruleKey()).thenReturn(rule2);

    inputFile1 = new DefaultInputFile(new DefaultIndexedFile("module1", baseDir, "project/path1", "de_DE"), x -> x.hash());
    inputFile2 = new DefaultInputFile(new DefaultIndexedFile("module2", baseDir, "project/path2", "de_DE"), x -> x.hash());
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
    assertNull(sqIssue1.line());
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
    InputComponent ic = new DefaultInputFile(new DefaultIndexedFile("module2", Paths.get("./src/test/java"), "project/path2", "de_DE"), x -> x.hash());

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
