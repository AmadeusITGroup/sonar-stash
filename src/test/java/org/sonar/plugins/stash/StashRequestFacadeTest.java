package org.sonar.plugins.stash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashClientException;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;
import org.sonar.plugins.stash.issue.CoverageIssue;
import org.sonar.plugins.stash.issue.CoverageIssuesReport;
import org.sonar.plugins.stash.issue.SonarQubeIssue;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;
import org.sonar.plugins.stash.issue.StashComment;
import org.sonar.plugins.stash.issue.StashCommentReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashPullRequest;
import org.sonar.plugins.stash.issue.StashTask;
import org.sonar.plugins.stash.issue.StashUser;

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
  
  SonarQubeIssuesReport issueReport;
  CoverageIssuesReport coverageReport;
  
  private static final String STASH_PROJECT = "Project";
  private static final String STASH_REPOSITORY = "Repository";
  private static final int STASH_PULLREQUEST_ID = 1;
  private static final PullRequestRef pr = PullRequestRef.builder()
          .setProject(STASH_PROJECT)
          .setRepository(STASH_REPOSITORY)
          .setPullRequestId(STASH_PULLREQUEST_ID)
          .build();

  private static final String STASH_DIFF_TYPE = "StashDiffType";
  private static final String STASH_USER = "SonarQube";
  
  private static final String SONARQUBE_URL = "http://sonar/url";
  
  private static final String FILE_PATH_1 = "path/to/file1"; 
  private static final String FILE_PATH_2 = "path/to/file2"; 
  
  @Before
  public void setUp() throws Exception {
    config = mock(StashPluginConfiguration.class);
    when(config.getTaskIssueSeverityThreshold()).thenReturn(StashPlugin.SEVERITY_NONE);
    
    
    StashRequestFacade facade = new StashRequestFacade(config);  
    myFacade = spy(facade);
    
    stashClient = mock(StashClient.class);
    
    diffReport = mock(StashDiffReport.class);
    
    when(diffReport.getType(anyString(), anyLong())).thenReturn(STASH_DIFF_TYPE);
    when(diffReport.getLine(FILE_PATH_1, 1)).thenReturn((long) 1);
    when(diffReport.getLine(FILE_PATH_1, 2)).thenReturn((long) 2);
    when(diffReport.getLine(FILE_PATH_2, 1)).thenReturn((long) 1);
    
    stashUser = mock(StashUser.class);
    when(stashUser.getId()).thenReturn((long) 1234);
    
    issueReport = new SonarQubeIssuesReport();
    
    SonarQubeIssue issue1 = new SonarQubeIssue("key1", Severity.CRITICAL, "message1", "rule1", FILE_PATH_1, 1);
    stashCommentMessage1 = issue1.printIssueMarkdown(SONARQUBE_URL);
    issueReport.add(issue1);
    
    SonarQubeIssue issue2 = new SonarQubeIssue("key2", Severity.MAJOR, "message2", "rule2", FILE_PATH_1, 2);
    stashCommentMessage2 = issue2.printIssueMarkdown(SONARQUBE_URL);
    issueReport.add(issue2);
    
    SonarQubeIssue issue3 = new SonarQubeIssue("key3", Severity.INFO, "message3", "rule3", FILE_PATH_2, 1);
    stashCommentMessage3 = issue3.printIssueMarkdown(SONARQUBE_URL);
    issueReport.add(issue3);
    
    CoverageIssue coverageIssue = new CoverageIssue(Severity.MAJOR, "sonar/coverage/file");
    
    coverageReport = new CoverageIssuesReport();
    coverageReport.add(coverageIssue);
    
    StashTask task1 = mock(StashTask.class);
    when(task1.getId()).thenReturn((long) 1111);
    
    List<StashTask> taskList1 = new ArrayList<>();
    taskList1.add(task1);
    
    comment1 = mock(StashComment.class);
    when(comment1.getId()).thenReturn((long) 1111);
    when(comment1.getAuthor()).thenReturn(stashUser);
    when(stashClient.postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE)).thenReturn(comment1);
    when(comment1.getTasks()).thenReturn(taskList1);
    when(comment1.containsPermanentTasks()).thenReturn(false);
    
    StashTask task2 = mock(StashTask.class);
    when(task1.getId()).thenReturn((long) 2222);
    
    List<StashTask> taskList2 = new ArrayList<>();
    taskList2.add(task2);
    
    comment2 = mock(StashComment.class);
    when(comment2.getId()).thenReturn((long) 2222);
    when(comment2.getAuthor()).thenReturn(stashUser);
    when(stashClient.postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE)).thenReturn(comment2);
    when(comment2.getTasks()).thenReturn(taskList2);
    when(comment2.containsPermanentTasks()).thenReturn(false);
    
    StashTask task3 = mock(StashTask.class);
    when(task3.getId()).thenReturn((long) 3333);
    
    List<StashTask> taskList3 = new ArrayList<>();
    taskList3.add(task3);
    
    comment3 = mock(StashComment.class);
    when(comment3.getId()).thenReturn((long) 3333);
    when(comment3.getAuthor()).thenReturn(stashUser);
    when(stashClient.postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE)).thenReturn(comment3);
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
    when(stashClient.getPullRequestComments(pr, FILE_PATH_1)).thenReturn(stashCommentsReport1);
    
    stashCommentsReport2 = mock(StashCommentReport.class);
    when(stashCommentsReport1.getComments()).thenReturn(comments);
    when(stashCommentsReport2.applyDiffReport(diffReport)).thenReturn(stashCommentsReport2);
    when(stashClient.getPullRequestComments(pr, FILE_PATH_2)).thenReturn(stashCommentsReport2);
    
    doNothing().when(stashClient).deletePullRequestComment(Mockito.eq(pr), (StashComment) Mockito.anyObject());
    doNothing().when(stashClient).deleteTaskOnComment((StashTask) Mockito.anyObject());
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

  @Test (expected = StashConfigurationException.class)
  public void testGetPasswordFromUnconfiguredEnvironment() throws StashConfigurationException {
    when(config.getStashLogin()).thenReturn("login");
    when(config.getStashPasswordEnvironmentVariable()).thenReturn("SONAR_STASH_PASSWORD");

    StashCredentials credentials = myFacade.getCredentials();
  }

  @Test
  public void testGetIssueThreshold() throws StashConfigurationException {
    when(config.getIssueThreshold()).thenReturn(1);
    assertEquals(myFacade.getIssueThreshold(), 1);
  }
  
  @Test (expected = StashConfigurationException.class)
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
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashURLThrowsException() throws StashConfigurationException {
    when(config.getStashURL()).thenReturn(null);
    myFacade.getStashURL();
  }
  
  @Test
  public void testGetStashProject() throws StashConfigurationException {
    when(config.getStashProject()).thenReturn("project");
    assertEquals(myFacade.getStashProject(), "project");
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashProjectThrowsException() throws StashConfigurationException {
    when(config.getStashProject()).thenReturn(null);
    myFacade.getStashProject();
  }
    
  @Test
  public void testGetStashRepository() throws StashConfigurationException {
    when(config.getStashRepository()).thenReturn("repository");
    assertEquals(myFacade.getStashRepository(), "repository");
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashRepositoryThrowsException() throws StashConfigurationException {
    when(config.getStashRepository()).thenReturn(null);
    myFacade.getStashRepository();
  }
 
  @Test
  public void testGetStashPullRequestId() throws StashConfigurationException {
    when(config.getPullRequestId()).thenReturn(12345);
    assertEquals(myFacade.getStashPullRequestId(), 12345);
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashPullRequestIdThrowsException() throws StashConfigurationException {
    when(config.getPullRequestId()).thenReturn(null);
    myFacade.getStashPullRequestId();
  }
  
  @Test
  public void testPostCommentPerIssue() throws Exception{
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(true);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);
    
    myFacade.postCommentPerIssue(pr, SONARQUBE_URL, issueReport.getIssues(), diffReport, stashClient);
    
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
  }
  
  @Test
  public void testPostCommentPerIssueWithNoStashCommentAlreadyPushed() throws Exception{
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(true);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(true);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(true);
    
    myFacade.postCommentPerIssue(pr, SONARQUBE_URL, issueReport.getIssues(), diffReport, stashClient);

    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
  }
  
  @Test
  public void testPostSonarQubeReport() throws StashClientException {
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    verify(myFacade, times(1)).postCommentPerIssue(eq(pr), eq(SONARQUBE_URL), (Collection) anyObject(), eq(diffReport), eq(stashClient));
  }
  
  @Test
  public void testPostSonarQubeReportWithException() throws StashClientException {
    doThrow(new StashClientException("StashClientException for Test")).when(myFacade)
      .postCommentPerIssue(eq(pr), eq(SONARQUBE_URL), (Collection) anyObject(), eq(diffReport), eq(stashClient));
  
    try {
      myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
      
    } catch (Exception e) {
      assertFalse("Unexpected Exception: postSonarQubeReport does not raised any StashClientException", true);
    }
  }

  @Test
  public void testPostCoverageReport() throws StashClientException {
    myFacade.postCoverageReport(pr, SONARQUBE_URL, coverageReport, diffReport, stashClient);
    verify(myFacade, times(1)).postCommentPerIssue(eq(pr), eq(SONARQUBE_URL), (Collection) anyObject(), eq(diffReport), eq(stashClient));
  }
  
  @Test
  public void testPostCoverageReportWithEmptyReport() throws StashClientException {
    myFacade.postCoverageReport(pr, SONARQUBE_URL, new CoverageIssuesReport(), diffReport, stashClient);
    verify(myFacade, times(0)).postCommentPerIssue(eq(pr), eq(SONARQUBE_URL), (Collection) anyObject(), eq(diffReport), eq(stashClient));
  }
  
  @Test
  public void testPostCoverageReportWithException() throws StashClientException {
    doThrow(new StashClientException("StashClientException for Test")).when(myFacade)
      .postCommentPerIssue(eq(pr), eq(SONARQUBE_URL), (Collection) anyObject(), eq(diffReport), eq(stashClient));
  
    try {
      myFacade.postCoverageReport(pr, SONARQUBE_URL, coverageReport, diffReport, stashClient);
      
    } catch (Exception e) {
      assertFalse("Unexpected Exception: postCoverageReport does not raised any StashClientException", true);
    }
  }
  
  @Test
  public void testPostTaskOnComment() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Severity.INFO);
    
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
    
    verify(stashClient, times(1)).postTaskOnComment("message3", (long) 3333);
    verify(stashClient, times(1)).postTaskOnComment("message2", (long) 2222);
    verify(stashClient, times(1)).postTaskOnComment("message1", (long) 1111);
  }

  @Test
  public void testPostTaskOnCommentWithRestrictedLevel() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Severity.MAJOR);
    
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
    
    verify(stashClient, times(0)).postTaskOnComment("message3", (long) 3333);
    verify(stashClient, times(1)).postTaskOnComment("message2", (long) 2222);
    verify(stashClient, times(1)).postTaskOnComment("message1", (long) 1111);
  }
  
  @Test
  public void testPostTaskOnCommentWithNotPresentLevel() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Severity.BLOCKER);
    
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
    
    verify(stashClient, times(0)).postTaskOnComment("message3", (long) 3333);
    verify(stashClient, times(0)).postTaskOnComment("message2", (long) 2222);
    verify(stashClient, times(0)).postTaskOnComment("message1", (long) 1111);
  }
  
  @Test
  public void testPostTaskOnCommentWithSeverityNone() throws Exception {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(StashPlugin.SEVERITY_NONE);
    
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
 
    verify(stashClient, times(0)).postTaskOnComment("message3", (long) 3333);
    verify(stashClient, times(0)).postTaskOnComment("message2", (long) 2222);
    verify(stashClient, times(0)).postTaskOnComment("message1", (long) 1111);
  }
  
  @Test
  public void testPostSonarQubeReportWithNoType() throws Exception{
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(false);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);
    
    when(diffReport.getType(FILE_PATH_1, 1)).thenReturn(null);
    when(diffReport.getType(FILE_PATH_1, 2)).thenReturn(STASH_DIFF_TYPE);
    when(diffReport.getType(FILE_PATH_2, 1)).thenReturn(null);
    
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
  }
  
  @Test
  public void testPostSonarQubeReportWithNoSonarQubeIssues() throws Exception{
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, new SonarQubeIssuesReport(), diffReport, stashClient);
    
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
  }
  
  @Test
  public void testPostSonarQubeReportWithExceptions() throws Exception {
    when(stashCommentsReport1.contains(stashCommentMessage1, FILE_PATH_1, 1)).thenReturn(false);
    when(stashCommentsReport1.contains(stashCommentMessage2, FILE_PATH_1, 2)).thenReturn(false);
    when(stashCommentsReport2.contains(stashCommentMessage3, FILE_PATH_2, 1)).thenReturn(false);
    
    doThrow(new StashClientException("StashClientException for Test")).when(stashClient)
      .postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    
    try {
    myFacade.postSonarQubeReport(pr, SONARQUBE_URL, issueReport, diffReport, stashClient);
    
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage1, FILE_PATH_1, 1, STASH_DIFF_TYPE);
    verify(stashClient, times(1)).postCommentLineOnPullRequest(pr, stashCommentMessage2, FILE_PATH_1, 2, STASH_DIFF_TYPE);
    verify(stashClient, times(0)).postCommentLineOnPullRequest(pr, stashCommentMessage3, FILE_PATH_2, 1, STASH_DIFF_TYPE);
    
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
    
    verify(stashClient, times(3)).deleteTaskOnComment((StashTask) Mockito.anyObject());
    verify(stashClient, times(3)).deletePullRequestComment(Mockito.eq(pr), (StashComment) Mockito.anyObject());
  }
  
  @Test
  public void testResetCommentsWithNotDeletableTasks() throws Exception {
    when(comment1.containsPermanentTasks()).thenReturn(true);
    
    myFacade.resetComments(pr, diffReport, stashUser, stashClient);
    
    verify(stashClient, times(2)).deleteTaskOnComment((StashTask) Mockito.anyObject());
    verify(stashClient, times(2)).deletePullRequestComment(eq(pr), (StashComment) Mockito.anyObject());
  }
  
  @Test
  public void testResetCommentsWithNoTasks() throws Exception {
    when(comment1.getTasks()).thenReturn(new ArrayList<StashTask>());
    
    myFacade.resetComments(pr, diffReport, stashUser, stashClient);
    
    verify(stashClient, times(2)).deleteTaskOnComment((StashTask) Mockito.anyObject());
    verify(stashClient, times(3)).deletePullRequestComment(eq(pr), (StashComment) Mockito.anyObject());
  }
  
  @Test
  public void testResetCommentsWithDifferentStashUsers() throws Exception {
    StashUser stashUser2 = mock(StashUser.class);
    when(stashUser2.getId()).thenReturn((long) 4321);
    
    StashComment comment = mock(StashComment.class);
    when(comment.getAuthor()).thenReturn(stashUser2);
    
    ArrayList<StashComment> comments = new ArrayList<>();
    comments.add(comment);
    
    when(diffReport.getComments()).thenReturn(comments);
    
    myFacade.resetComments(pr, diffReport, stashUser, stashClient);
    
    verify(stashClient, times(0)).deletePullRequestComment(eq(pr), (StashComment) Mockito.anyObject());
  }
  
  @Test
  public void testResetCommentsWithoutAnyComments() throws Exception {
    when(diffReport.getComments()).thenReturn(new ArrayList<StashComment>());
    
    myFacade.resetComments(pr, diffReport, stashUser, stashClient);
    
    verify(stashClient, times(0)).deletePullRequestComment(eq(pr), (StashComment) Mockito.anyObject());
  }

  @Test
  public void testApprovePullRequest() throws Exception {
    myFacade.approvePullRequest(pr, stashClient);
    verify(stashClient, times(1)).approvePullRequest(pr);
  }
    
  @Test
  public void addResetPullRequestApproval() throws Exception {
    myFacade.resetPullRequestApproval(pr, stashClient);
    verify(stashClient, times(1)).resetPullRequestApproval(pr);
  }
  
  @Test
  public void testAddPullRequestReviewer() throws Exception {
    ArrayList<StashUser> reviewers = new ArrayList<>();
    StashUser stashUser = mock(StashUser.class);
    reviewers.add(stashUser);
    
    StashPullRequest pullRequest = mock(StashPullRequest.class);
    when(pullRequest.getReviewer(STASH_USER)).thenReturn(null);
    when(pullRequest.getVersion()).thenReturn((long) 1);
    
    when(stashClient.getPullRequest(pr)).thenReturn(pullRequest);
    when(stashClient.getUser(STASH_USER)).thenReturn(stashUser);
    
    myFacade.addPullRequestReviewer(pr, STASH_USER, stashClient);
    
    verify(stashClient, times(1)).addPullRequestReviewer(pr, (long) 1, reviewers);
  }
  
  @Test
  public void testAddPullRequestReviewerWithReviewerAlreadyAdded() throws Exception {
    ArrayList<StashUser> reviewers = new ArrayList<>();
    StashUser stashUser = mock(StashUser.class);
    reviewers.add(stashUser);
    
    StashPullRequest pullRequest = mock(StashPullRequest.class);
    when(pullRequest.getReviewer(STASH_USER)).thenReturn(stashUser);
    when(pullRequest.getVersion()).thenReturn((long) 1);
    
    when(stashClient.getPullRequest(pr)).thenReturn(pullRequest);
    when(stashClient.getUser(STASH_USER)).thenReturn(stashUser);
    
    myFacade.addPullRequestReviewer(pr, STASH_USER, stashClient);
    
    verify(stashClient, times(0)).addPullRequestReviewer(pr, (long) 1, reviewers);
  }
  
  @Test
  public void testGetReportedSeverities() {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Severity.INFO);
    
    List<String> severities = myFacade.getReportedSeverities();
    
    assertEquals(5, severities.size());
    assertEquals(StashPlugin.SEVERITY_LIST.get(0), severities.get(0));
    assertEquals(StashPlugin.SEVERITY_LIST.get(1), severities.get(1));
    assertEquals(StashPlugin.SEVERITY_LIST.get(2), severities.get(2));
    assertEquals(StashPlugin.SEVERITY_LIST.get(3), severities.get(3));
    assertEquals(StashPlugin.SEVERITY_LIST.get(4), severities.get(4));
  }
  
  @Test
  public void testGetReportedSeveritiesWithRestriction() {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(Severity.MAJOR);
    
    List<String> severities = myFacade.getReportedSeverities();
    
    assertEquals(3, severities.size());
    assertEquals(StashPlugin.SEVERITY_LIST.get(2), severities.get(0));
    assertEquals(StashPlugin.SEVERITY_LIST.get(3), severities.get(1));
    assertEquals(StashPlugin.SEVERITY_LIST.get(4), severities.get(2));
  }
  
  @Test
  public void testGetReportedSeveritiesWithNoSeverity() {
    when(config.getTaskIssueSeverityThreshold()).thenReturn(StashPlugin.SEVERITY_NONE);
    
    List<String> severities = myFacade.getReportedSeverities();
    
    assertEquals(0, severities.size());
  }
}
