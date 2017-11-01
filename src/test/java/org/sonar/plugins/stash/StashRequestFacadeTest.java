package org.sonar.plugins.stash;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.stash.StashPlugin.IssueType;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.fixtures.DummyIssuePathResolver;
import org.sonar.plugins.stash.issue.MarkdownPrinter;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class StashRequestFacadeTest extends StashTest {

  @Spy
  StashRequestFacade myFacade;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Mock
  StashPluginConfiguration config;

  @Mock
  StashClient stashClient;

  @Mock
  StashDiffReport diffReport;

  @Mock
  StashCommentReport stashCommentsReport1;

  @Mock
  StashCommentReport stashCommentsReport2;

  @Mock
  StashComment comment1;

  @Mock
  StashComment comment2;

  @Mock
  StashComment comment3;

  @Mock
  StashUser stashUser;

  String stashCommentMessage1;
  String stashCommentMessage2;
  String stashCommentMessage3;

  List<PostJobIssue> report;

  IssuePathResolver ipr;
  StashProjectBuilder spr;

  private static final String STASH_PROJECT = "Project";
  private static final String STASH_REPOSITORY = "Repository";
  private static final int STASH_PULLREQUEST_ID = 1;
  private static final PullRequestRef pr = PullRequestRef.builder()
                                                         .setProject(STASH_PROJECT)
                                                         .setRepository(STASH_REPOSITORY)
                                                         .setPullRequestId(STASH_PULLREQUEST_ID)
                                                         .build();

  private static final IssueType STASH_DIFF_TYPE = IssueType.CONTEXT;
  private static final String STASH_USER = "SonarQube";
  private static final String STASH_URL = "http://stash/url";

  private static final String SONARQUBE_URL = "http://sonar/url";

  private static final String FILE_PATH_1 = "path/to/file1";
  private static final String FILE_PATH_2 = "path/to/file2";

  @Before
  public void setUp() throws Exception {

    config = mock(StashPluginConfiguration.class);
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Optional.empty());
    when(config.getSonarQubeURL()).thenReturn(SONARQUBE_URL);
    when(config.getStashURL()).thenReturn(STASH_URL);
    when(config.getStashProject()).thenReturn(STASH_PROJECT);
    when(config.getStashRepository()).thenReturn(STASH_REPOSITORY);
    when(config.getRepositoryRoot()).thenReturn(Optional.empty());

    ActiveRules activeRules = new ActiveRulesBuilder().build();

    ipr = new DummyIssuePathResolver();
    spr = new DummyStashProjectBuilder(new File("/basedir"));

    StashRequestFacade facade = new StashRequestFacade(config, spr);
    myFacade = spy(facade);

    stashClient = mock(StashClient.class);

    diffReport = mock(StashDiffReport.class);

    when(diffReport.getType(anyString(), anyLong(), anyInt())).thenReturn(STASH_DIFF_TYPE);
    when(diffReport.getLine(FILE_PATH_1, 1)).thenReturn((long)1);
    when(diffReport.getLine(FILE_PATH_1, 2)).thenReturn((long)2);
    when(diffReport.getLine(FILE_PATH_2, 1)).thenReturn((long)1);

    stashUser = mock(StashUser.class);
    when(stashUser.getId()).thenReturn((long) 1234);

    MarkdownPrinter printer = new MarkdownPrinter(ipr, "http://stash/url", pr, 100,
        SONARQUBE_URL);

    report = new ArrayList<PostJobIssue>();

    Path moduleBaseDir = FileSystems.getDefault().getPath("some", "dir");

    PostJobIssue issue1 = new DefaultIssue().setKey("key1")
                                     .setSeverity(Severity.CRITICAL)
                                     .setMessage("message1")
                                     .setRuleKey(RuleKey.of("foo", "rule1"))
                                     .setInputComponent(new DefaultInputFile("module1", "/basedir/" + FILE_PATH_1)
                                         .setModuleBaseDir(moduleBaseDir))
                                     .setLine(1);
    stashCommentMessage1 = printer.printIssueMarkdown(issue1);
    report.add(issue1);

    PostJobIssue issue2 = new DefaultIssue().setKey("key2")
                                     .setSeverity(Severity.MAJOR)
                                     .setMessage("message2")
                                     .setRuleKey(RuleKey.of("foo", "rule2"))
                                     .setInputComponent(new DefaultInputFile("module2", "/basedir/" + FILE_PATH_1)
                                         .setModuleBaseDir(moduleBaseDir))
                                     .setLine(2);
    stashCommentMessage2 = printer.printIssueMarkdown(issue2);
    report.add(issue2);


    PostJobIssue issue3 = new DefaultIssue().setKey("key3")
                                     .setSeverity(Severity.INFO)
                                     .setMessage("message3")
                                     .setRuleKey(RuleKey.of("foo", "rule3"))
                                     .setInputComponent(new DefaultInputFile("module3", "/basedir/" + FILE_PATH_2)
                                         .setModuleBaseDir(moduleBaseDir))
                                     .setLine(1);
    stashCommentMessage3 = printer.printIssueMarkdown(issue3);
    report.add(issue3);

    StashTask task1 = mock(StashTask.class);
    when(task1.getId()).thenReturn((long)1111);

    List<StashTask> taskList1 = new ArrayList<>();
    taskList1.add(task1);

    comment1 = mock(StashComment.class);
    when(comment1.getId()).thenReturn((long)1111);
    when(comment1.getAuthor()).thenReturn(stashUser);
    when(stashClient.postCommentLineOnPullRequest(pr,
                                                  stashCommentMessage1,
                                                  FILE_PATH_1,
                                                  1,
                                                  STASH_DIFF_TYPE)).thenReturn(comment1);
    when(comment1.getTasks()).thenReturn(taskList1);
    when(comment1.containsPermanentTasks()).thenReturn(false);

    StashTask task2 = mock(StashTask.class);
    when(task1.getId()).thenReturn((long)2222);

    List<StashTask> taskList2 = new ArrayList<>();
    taskList2.add(task2);

    comment2 = mock(StashComment.class);
    when(comment2.getId()).thenReturn((long)2222);
    when(comment2.getAuthor()).thenReturn(stashUser);
    when(stashClient.postCommentLineOnPullRequest(pr,
                                                  stashCommentMessage2,
                                                  FILE_PATH_1,
                                                  2,
                                                  STASH_DIFF_TYPE)).thenReturn(comment2);
    when(comment2.getTasks()).thenReturn(taskList2);
    when(comment2.containsPermanentTasks()).thenReturn(false);

    StashTask task3 = mock(StashTask.class);
    when(task3.getId()).thenReturn((long)3333);

    List<StashTask> taskList3 = new ArrayList<>();
    taskList3.add(task3);

    comment3 = mock(StashComment.class);
    when(comment3.getId()).thenReturn((long)3333);
    when(comment3.getAuthor()).thenReturn(stashUser);
    when(stashClient.postCommentLineOnPullRequest(pr,
                                                  stashCommentMessage3,
                                                  FILE_PATH_2,
                                                  1,
                                                  STASH_DIFF_TYPE)).thenReturn(comment3);
    when(comment3.getTasks()).thenReturn(taskList3);
    when(comment3.containsPermanentTasks()).thenReturn(false);

    ArrayList<StashComment> comments = new ArrayList<>();
    comments.add(comment1);
    comments.add(comment2);
    comments.add(comment3);

    when(diffReport.getComments()).thenReturn(comments);

    stashCommentsReport1 = mock(StashCommentReport.class);
    when(stashCommentsReport1.getComments()).thenReturn(comments);
    when(stashCommentsReport1.applyDiffReport(diffReport)).thenReturn(stashCommentsReport1);
    when(stashClient.getPullRequestComments(pr, "path/to/file1")).thenReturn(stashCommentsReport1);

    stashCommentsReport2 = mock(StashCommentReport.class);
    when(stashCommentsReport1.getComments()).thenReturn(comments);
    when(stashCommentsReport2.applyDiffReport(diffReport)).thenReturn(stashCommentsReport2);
    when(stashClient.getPullRequestComments(pr, "path/to/file2")).thenReturn(stashCommentsReport2);

    doNothing().when(stashClient).deletePullRequestComment(Mockito.eq(pr), (StashComment)Mockito.anyObject());
    doNothing().when(stashClient).deleteTaskOnComment((StashTask)Mockito.anyObject());
  }

  @Test
  public void testGetCredentials() throws StashConfigurationException {
    when(config.getStashLogin()).thenReturn("login");
    when(config.getStashPassword()).thenReturn("password");

    StashCredentials credentials = myFacade.getCredentials();
    assertEquals(credentials.getLogin(), "login");
    assertEquals(credentials.getPassword(), "password");
  }

  @Test
  public void testGetNoCredentials() throws StashConfigurationException {
    when(config.getStashLogin()).thenReturn(null);
    when(config.getStashPassword()).thenReturn(null);

    StashCredentials credentials = myFacade.getCredentials();
    assertNull(credentials.getLogin());
    assertNull(credentials.getPassword());
  }

  @Test
  public void testGetPasswordFromEnvironment() throws StashConfigurationException {
    when(config.getStashLogin()).thenReturn("login");
    when(config.getStashPasswordEnvironmentVariable()).thenReturn("SONAR_STASH_PASSWORD");
    environmentVariables.set("SONAR_STASH_PASSWORD", "envPassword");

    StashCredentials credentials = myFacade.getCredentials();
    assertEquals(credentials.getPassword(), "envPassword");

    when(config.getStashPassword()).thenReturn("password");
    credentials = myFacade.getCredentials();
    assertEquals(credentials.getPassword(), "envPassword");
  }

  @Test(expected = StashConfigurationException.class)
  public void testGetPasswordFromUnconfiguredEnvironment() throws StashConfigurationException {
    when(config.getStashLogin()).thenReturn("login");
    when(config.getStashPasswordEnvironmentVariable()).thenReturn("SONAR_STASH_PASSWORD");

    myFacade.getCredentials();
  }

  @Test
  public void testGetIssueThreshold() throws StashConfigurationException {
    when(config.getIssueThreshold()).thenReturn(1);
    assertEquals(myFacade.getIssueThreshold(), 1);
  }

  @Test(expected = StashConfigurationException.class)
  public void testGetIssueThresholdThrowsException() throws StashConfigurationException {
    when(config.getIssueThreshold()).thenThrow(new NumberFormatException());
    myFacade.getIssueThreshold();
  }

  @Test
  public void testGetStashURL() throws StashConfigurationException {
    when(config.getStashURL()).thenReturn("http://url");
    assertEquals(myFacade.getStashURL(), "http://url");

    when(config.getStashURL()).thenReturn("http://url/");
    assertEquals(myFacade.getStashURL(), "http://url");
  }

  @Test(expected = StashConfigurationException.class)
  public void testGetStashURLThrowsException() throws StashConfigurationException {
    when(config.getStashURL()).thenReturn(null);
    myFacade.getStashURL();
  }

  @Test
  public void testGetStashProject() throws StashConfigurationException {
    when(config.getStashProject()).thenReturn("project");
    assertEquals(myFacade.getStashProject(), "project");
  }

  @Test(expected = StashConfigurationException.class)
  public void testGetStashProjectThrowsException() throws StashConfigurationException {
    when(config.getStashProject()).thenReturn(null);
    myFacade.getStashProject();
  }

  @Test
  public void testGetStashRepository() throws StashConfigurationException {
    when(config.getStashRepository()).thenReturn("repository");
    assertEquals(myFacade.getStashRepository(), "repository");
  }

  @Test(expected = StashConfigurationException.class)
  public void testGetStashRepositoryThrowsException() throws StashConfigurationException {
    when(config.getStashRepository()).thenReturn(null);
    myFacade.getStashRepository();
  }

  @Test
  public void testGetStashPullRequestId() throws StashConfigurationException {
    when(config.getPullRequestId()).thenReturn(12345);
    assertEquals(myFacade.getStashPullRequestId(), 12345);
  }

  @Test(expected = StashConfigurationException.class)
  public void testGetStashPullRequestIdThrowsException() throws StashConfigurationException {
    when(config.getPullRequestId()).thenReturn(null);
    myFacade.getStashPullRequestId();
  }

  @Test
  public void testPostCommentPerIssue() throws Exception {
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(true);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);

    myFacade.postCommentPerIssue(pr, report, diffReport, stashClient);

    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               "path/to/file1",
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               "path/to/file2",
                                                               1,
                                                               STASH_DIFF_TYPE);
  }

  @Test
  public void testPostCommentPerIssueWithNoStashCommentAlreadyPushed() throws Exception {
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(true);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(true);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(true);

    myFacade.postCommentPerIssue(pr, report, diffReport, stashClient);

    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);
  }

  @Test
  public void testPostSonarQubeReport() throws StashClientException, StashConfigurationException {
    myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);
    verify(myFacade, times(1)).postCommentPerIssue(eq(pr),
                                                   anyCollectionOf(PostJobIssue.class),
                                                   eq(diffReport),
                                                   eq(stashClient));
  }

  @Test
  public void testPostSonarQubeReportWithException()
      throws StashClientException, StashConfigurationException {
    doThrow(new StashClientException("StashClientException for Test")).when(myFacade)
                                                                      .postCommentPerIssue(eq(pr),
                                                                                           anyCollectionOf(PostJobIssue.class),
                                                                                           eq(diffReport),
                                                                                           eq(stashClient));

    try {
      myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

    } catch (Exception e) {
      assertFalse("Unexpected Exception: postSonarQubeReport does not raised any StashClientException", true);
    }
  }

  @Test
  public void testPostTaskOnComment() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Optional.of(Severity.INFO));

    myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);

    verify(stashClient, times(1)).postTaskOnComment("message3", (long)3333);
    verify(stashClient, times(1)).postTaskOnComment("message2", (long)2222);
    verify(stashClient, times(1)).postTaskOnComment("message1", (long)1111);
  }

  @Test
  public void testPostTaskOnCommentWithRestrictedLevel() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Optional.of(Severity.MAJOR));

    myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);

    verify(stashClient, times(0)).postTaskOnComment("message3", (long)3333);
    verify(stashClient, times(1)).postTaskOnComment("message2", (long)2222);
    verify(stashClient, times(1)).postTaskOnComment("message1", (long)1111);
  }

  @Test
  public void testPostTaskOnCommentWithNotPresentLevel() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Optional.of(Severity.BLOCKER));

    myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);

    verify(stashClient, times(0)).postTaskOnComment("message3", (long)3333);
    verify(stashClient, times(0)).postTaskOnComment("message2", (long)2222);
    verify(stashClient, times(0)).postTaskOnComment("message1", (long)1111);
  }

  @Test
  public void testPostTaskOnCommentWithSeverityNone() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Optional.empty());

    myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);

    verify(stashClient, times(0)).postTaskOnComment("message3", (long)3333);
    verify(stashClient, times(0)).postTaskOnComment("message2", (long)2222);
    verify(stashClient, times(0)).postTaskOnComment("message1", (long)1111);
  }

  @Test
  public void testPostSonarQubeReportWithNoType() throws Exception {
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(false);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);

    when(diffReport.getType(FILE_PATH_1, 1, config.issueVicinityRange())).thenReturn(null);
    when(diffReport.getType(FILE_PATH_1, 2, config.issueVicinityRange())).thenReturn(STASH_DIFF_TYPE);
    when(diffReport.getType(FILE_PATH_2, 1, config.issueVicinityRange())).thenReturn(null);

    myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);
  }

  @Test
  public void testPostSonarQubeReportWithNoSonarQubeIssues() throws Exception {
    myFacade.postSonarQubeReport(pr, new ArrayList<>(), diffReport, stashClient);

    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage1,
                                                               FILE_PATH_1,
                                                               1,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage2,
                                                               FILE_PATH_1,
                                                               2,
                                                               STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                               stashCommentMessage3,
                                                               FILE_PATH_2,
                                                               1,
                                                               STASH_DIFF_TYPE);
  }

  @Test
  public void testPostSonarQubeReportWithExceptions() throws Exception {
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(false);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);

    doThrow(new StashClientException("StashClientException for Test")).when(stashClient)
                                                                      .postCommentLineOnPullRequest(pr,
                                                                                                    stashCommentMessage2,
                                                                                                    FILE_PATH_1,
                                                                                                    2,
                                                                                                    STASH_DIFF_TYPE);

    try {
      myFacade.postSonarQubeReport(pr, report, diffReport, stashClient);

      verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                                 stashCommentMessage1,
                                                                 FILE_PATH_1,
                                                                 1,
                                                                 STASH_DIFF_TYPE);
      verify(stashClient, times(1)).postCommentLineOnPullRequest(pr,
                                                                 stashCommentMessage2,
                                                                 FILE_PATH_1,
                                                                 2,
                                                                 STASH_DIFF_TYPE);
      verify(stashClient, times(0)).postCommentLineOnPullRequest(pr,
                                                                 stashCommentMessage3,
                                                                 FILE_PATH_2,
                                                                 1,
                                                                 STASH_DIFF_TYPE);

    } catch (StashClientException e) {
      assertFalse("Unexpected Exception: postCommentLineOnPullRequest does not raised any StashClientException", true);
    }
  }

  @Test
  public void testGetSonarQubeReviewer() throws Exception {
    when(stashClient.getUser(STASH_USER)).thenReturn(stashUser);

    StashUser reviewer = myFacade.getSonarQubeReviewer(STASH_USER, stashClient);
    assertEquals(reviewer.getId(), 1234);
  }

  @Test
  public void testGetSonarQubeReviewerWithException() throws Exception {
    doThrow(new StashClientException("StashClientException for Test")).when(stashClient).getUser(STASH_USER);

    StashUser reviewer = myFacade.getSonarQubeReviewer(STASH_USER, stashClient);
    assertEquals(reviewer, null);
  }

  @Test
  public void testGetPullRequestDiffReport() throws Exception {
    when(stashClient.getPullRequestDiffs(pr)).thenReturn(diffReport);

    StashDiffReport result = myFacade.getPullRequestDiffReport(pr, stashClient);

    assertEquals(result.getLine(FILE_PATH_1, 1), 1);
    assertEquals(result.getLine(FILE_PATH_1, 2), 2);
    assertEquals(result.getLine(FILE_PATH_2, 1), 1);
  }

  @Test
  public void testGetPullRequestDiffReportWithException() throws Exception {
    doThrow(new StashClientException("StashClientException for Test")).when(stashClient)
                                                                      .getPullRequestDiffs(pr);

    StashDiffReport result = myFacade.getPullRequestDiffReport(pr, stashClient);
    assertEquals(result, null);
  }

  @Test
  public void testResetComments() throws Exception {
    myFacade.resetComments(pr, diffReport, stashUser, stashClient);

    verify(stashClient, times(3)).deleteTaskOnComment((StashTask)Mockito.anyObject());
    verify(stashClient, times(3)).deletePullRequestComment(Mockito.eq(pr), (StashComment)Mockito.anyObject());
  }

  @Test
  public void testResetCommentsWithNotDeletableTasks() throws Exception {
    when(comment1.containsPermanentTasks()).thenReturn(true);

    myFacade.resetComments(pr, diffReport, stashUser, stashClient);

    verify(stashClient, times(2)).deleteTaskOnComment((StashTask)Mockito.anyObject());
    verify(stashClient, times(2)).deletePullRequestComment(eq(pr), (StashComment)Mockito.anyObject());
  }

  @Test
  public void testResetCommentsWithNoTasks() throws Exception {
    when(comment1.getTasks()).thenReturn(new ArrayList<StashTask>());

    myFacade.resetComments(pr, diffReport, stashUser, stashClient);

    verify(stashClient, times(2)).deleteTaskOnComment((StashTask)Mockito.anyObject());
    verify(stashClient, times(3)).deletePullRequestComment(eq(pr), (StashComment)Mockito.anyObject());
  }

  @Test
  public void testResetCommentsWithDifferentStashUsers() throws Exception {
    StashUser stashUser2 = mock(StashUser.class);
    when(stashUser2.getId()).thenReturn((long)4321);

    StashComment comment = mock(StashComment.class);
    when(comment.getAuthor()).thenReturn(stashUser2);

    ArrayList<StashComment> comments = new ArrayList<>();
    comments.add(comment);

    when(diffReport.getComments()).thenReturn(comments);

    myFacade.resetComments(pr, diffReport, stashUser, stashClient);

    verify(stashClient, times(0)).deletePullRequestComment(eq(pr), (StashComment)Mockito.anyObject());
  }

  @Test
  public void testResetCommentsWithoutAnyComments() throws Exception {
    when(diffReport.getComments()).thenReturn(new ArrayList<StashComment>());

    myFacade.resetComments(pr, diffReport, stashUser, stashClient);

    verify(stashClient, times(0)).deletePullRequestComment(eq(pr), (StashComment)Mockito.anyObject());
  }


  @Test
  public void testResetCommentException() throws Exception {
    when(comment1.getTasks()).thenReturn(new ArrayList<StashTask>());

    StashClient StaCli = mock(StashClient.class);
    doThrow(StashClientException.class).when(StaCli).deletePullRequestComment(any(), any());

    myFacade.resetComments(pr, diffReport, stashUser, StaCli);
  }


  @Test
  public void testApprovePullRequest() throws Exception {

    myFacade.approvePullRequest(pr, stashClient);
    verify(stashClient, times(1)).approvePullRequest(pr);
  }


  @Test
  public void testApprovePullRequestException() throws Exception {

    StashClient StaCli = mock(StashClient.class);
    doThrow(StashClientException.class).when(StaCli).approvePullRequest(any());

    myFacade.approvePullRequest(pr, StaCli);
  }


  @Test
  public void addResetPullRequestApproval() throws Exception {

    myFacade.resetPullRequestApproval(pr, stashClient);
    verify(stashClient, times(1)).resetPullRequestApproval(pr);
  }


  @Test
  public void addResetPullRequestApprovalException() throws Exception {

    StashClient StaCli = mock(StashClient.class);
    doThrow(StashClientException.class).when(StaCli).resetPullRequestApproval(any());

    myFacade.resetPullRequestApproval(pr, StaCli);
  }


  @Test
  public void testAddPullRequestReviewer() throws Exception {
    ArrayList<StashUser> reviewers = new ArrayList<>();
    StashUser stashUser = mock(StashUser.class);
    reviewers.add(stashUser);

    StashPullRequest pullRequest = mock(StashPullRequest.class);
    when(pullRequest.getReviewer(STASH_USER)).thenReturn(null);
    when(pullRequest.getVersion()).thenReturn((long)1);

    when(stashClient.getPullRequest(pr)).thenReturn(pullRequest);
    when(stashClient.getUser(STASH_USER)).thenReturn(stashUser);

    myFacade.addPullRequestReviewer(pr, STASH_USER, stashClient);

    verify(stashClient, times(1)).addPullRequestReviewer(pr, 1, reviewers);
  }


  @Test
  public void testAddPullRequestReviewerException() throws Exception {

    StashClient StaCli = mock(StashClient.class);
    doThrow(StashClientException.class).when(StaCli).getPullRequest(any());

    myFacade.addPullRequestReviewer(pr, STASH_USER, StaCli);
  }


  @Test
  public void testAddPullRequestReviewerWithReviewerAlreadyAdded() throws Exception {
    ArrayList<StashUser> reviewers = new ArrayList<>();
    StashUser stashUser = mock(StashUser.class);
    reviewers.add(stashUser);

    StashPullRequest pullRequest = mock(StashPullRequest.class);
    when(pullRequest.getReviewer(STASH_USER)).thenReturn(stashUser);
    when(pullRequest.getVersion()).thenReturn((long)1);

    when(stashClient.getPullRequest(pr)).thenReturn(pullRequest);
    when(stashClient.getUser(STASH_USER)).thenReturn(stashUser);

    myFacade.addPullRequestReviewer(pr, STASH_USER, stashClient);

    verify(stashClient, times(0)).addPullRequestReviewer(pr, 1, reviewers);
  }


  @Test
  public void testGetPullRequest() throws Exception {

    doReturn("SonarQube").when(myFacade).getStashProject();
    doReturn("sonar-stash-plugin").when(myFacade).getStashRepository();
    doReturn(1337).when(myFacade).getStashPullRequestId();

    assertTrue(myFacade.getPullRequest() instanceof PullRequestRef);
  }


  /* FIXME
  @Test
  public void testGetIssuePathWithoutExplicitSourceRootDir() {
    when(config.getRepositoryRoot()).thenReturn(Optional.empty());

    inputFileCache.putInputFile(
        "key",
        new DefaultInputFile("some/relative/path").setAbsolutePath("/root/some/absolute/path")
    );
    assertEquals("some/absolute/path",
                 myFacade.getIssuePath(new DefaultIssue().setComponentKey("key")));


  }

  @Test
  public void testGetIssuePathWithExplicitSourceRootDir() {
    when(config.getRepositoryRoot()).thenReturn(Optional.of(new File("/root/some/")));

    inputFileCache.putInputFile(
        "key",
        new DefaultInputFile("some/relative/path").setAbsolutePath("/root/some/absolute/path")
    );
    assertEquals("absolute/path",
                 myFacade.getIssuePath(new DefaultIssue().setComponentKey("key")));


  }
  */

  @Test
  public void testPostCommentPerIssueWithIncludeVicinityIssues() throws Exception {
    when(config.issueVicinityRange()).thenReturn(10);
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(false);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);
    when(diffReport.getType(FILE_PATH_1, 1, config.issueVicinityRange())).thenReturn(STASH_DIFF_TYPE);
    when(diffReport.getType(FILE_PATH_1, 2, config.issueVicinityRange())).thenReturn(STASH_DIFF_TYPE);
    when(diffReport.getType(FILE_PATH_2, 1, config.issueVicinityRange())).thenReturn(STASH_DIFF_TYPE);

    myFacade.postCommentPerIssue(pr, report, diffReport, stashClient);

    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
  }
}
