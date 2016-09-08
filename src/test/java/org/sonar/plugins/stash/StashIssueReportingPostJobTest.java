package org.sonar.plugins.stash;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.Severity;
import org.sonar.plugins.stash.client.SonarQubeClient;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.issue.CoverageIssuesReport;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;


public class StashIssueReportingPostJobTest extends StashTest {
  
  StashIssueReportingPostJob myJob;
  
  @Mock
  StashUser stashUser;
  
  @Mock
  StashRequestFacade stashRequestFacade;
  
  @Mock
  StashPluginConfiguration config;
  
  @Mock
  ProjectIssues projectIssues;
  
  @Mock
  InputFileCache inputFileCache;
  
  @Mock
  InputFileCacheSensor inputFileCacheSensor;
  
  @Mock
  Project project;
  
  @Mock
  StashDiffReport diffReport;
  
  @Mock
  SonarQubeIssuesReport sqReport;
  
  @Mock
  CoverageIssuesReport coverageReport;
  
  @Mock
  SensorContext context;
  
  private static final String STASH_PROJECT = "Project";
  private static final String STASH_REPOSITORY = "Repository";
  private static final String STASH_PULLREQUEST_ID = "1";
  private static final String STASH_LOGIN = "login";
  private static final String STASH_PASSWORD = "password";
  private static final String STASH_URL = "http://url/to/stash";
  private static final int STASH_TIMEOUT = 10000;
  private static final int STASH_ISSUE_THRESHOLD = 100;
  
  private static final String SONARQUBE_URL = "http://url/to/sonarqube";
  
  private static final String[] ID_CARD = {STASH_PROJECT, STASH_REPOSITORY, STASH_PULLREQUEST_ID, SONARQUBE_URL, STASH_URL};
  
  @Before
  public void setUp() throws Exception {
    projectIssues = mock(ProjectIssues.class);
    inputFileCache = mock(InputFileCache.class);
    inputFileCacheSensor = mock(InputFileCacheSensor.class);
    
    project = new Project("1");
    context = mock(SensorContext.class);
    
    stashUser = mock(StashUser.class);
    
    diffReport = mock(StashDiffReport.class);
    
    config = mock(StashPluginConfiguration.class);
    when(config.hasToNotifyStash()).thenReturn(true);
    when(config.canApprovePullRequest()).thenReturn(false);
    when(config.getStashURL()).thenReturn(STASH_URL);
    when(config.getSonarQubeURL()).thenReturn(SONARQUBE_URL);
    when(config.getStashTimeout()).thenReturn(STASH_TIMEOUT);
    when(config.resetComments()).thenReturn(false);
    when(config.hasToNotifyStash()).thenReturn(true);
   
    stashRequestFacade = mock(StashRequestFacade.class);
    
    sqReport = mock(SonarQubeIssuesReport.class);
    when(sqReport.countIssues()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(sqReport);
    
    coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(5);
    when(coverageReport.getEvolution()).thenReturn(10.0);
    
    when(stashRequestFacade.getIssueThreshold()).thenReturn(STASH_ISSUE_THRESHOLD);
    when(stashRequestFacade.getStashProject()).thenReturn(STASH_PROJECT);
    when(stashRequestFacade.getStashRepository()).thenReturn(STASH_REPOSITORY);
    when(stashRequestFacade.getStashPullRequestId()).thenReturn(STASH_PULLREQUEST_ID);
    when(stashRequestFacade.getCredentials()).thenReturn(new StashCredentials(STASH_LOGIN, STASH_PASSWORD));
    when(stashRequestFacade.getSonarQubeReviewer(Mockito.anyString(), (StashClient) Mockito.anyObject())).thenReturn(stashUser);
    when(stashRequestFacade.getPullRequestDiffReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), (StashClient) Mockito.anyObject())).thenReturn(diffReport);
    when(stashRequestFacade.getIssueThreshold()).thenReturn(STASH_ISSUE_THRESHOLD);
    when(stashRequestFacade.getStashURL()).thenReturn(STASH_URL);
    
    when(stashRequestFacade.getCodeCoverageSeverity()).thenReturn(Severity.INFO);
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq(Severity.INFO), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    when(config.includeAnalysisOverview()).thenReturn(Boolean.TRUE);
  }
  
  @Test
  public void testExecuteOn() throws Exception {
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(sqReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(sqReport), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithReachedThreshold() throws Exception {
    when(stashRequestFacade.getIssueThreshold()).thenReturn(100);
    
    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(55);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);
    
    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(50);
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq(Severity.INFO), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
   }
  
  @Test
  public void testExecuteOnWithNoPluginActivation() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(false);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(sqReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(sqReport), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
 }
  
  @Test
  public void testExecuteOnWithNoStashUserDefined() throws Exception {
    when(stashRequestFacade.getSonarQubeReviewer(Mockito.anyString(), (StashClient) Mockito.anyObject())).thenReturn(null);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(sqReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(sqReport), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
 }
  
  @Test
  public void testExecuteOnWithResetCommentActivated() throws Exception {
    when(config.resetComments()).thenReturn(true);

    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(1)).resetComments(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(sqReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(sqReport), eq(coverageReport), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithNoDiffReport() throws Exception {
    diffReport = null;
    when(stashRequestFacade.getPullRequestDiffReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), (StashClient) Mockito.anyObject())).thenReturn(diffReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(sqReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(sqReport), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithPullRequestApprovalAndNoNewIssueAndCodeCoverageEvolutionPositive() throws Exception {
    when(config.canApprovePullRequest()).thenReturn(true);
    
    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(0);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);
    
    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(0);
    when(coverageReport.getEvolution()).thenReturn(10.0);
    
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq("INFO"), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithPullRequestApprovalAndNoNewIssueAndCodeCoverageEvolutionNegative() throws Exception {
    when(config.canApprovePullRequest()).thenReturn(true);
    
    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(0);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);
    
    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(0);
    when(coverageReport.getEvolution()).thenReturn(-10.0);
    
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq("INFO"), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
  }
  
  
  @Test
  public void testExecuteOnWithPullRequestApprovalAndNewIssueAndCodeCoverageEvolutionPositive() throws Exception {
    when(config.canApprovePullRequest()).thenReturn(true);
    
    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);
    
    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(0);
    when(coverageReport.getEvolution()).thenReturn(10.0);
    
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq("INFO"), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithoutPullRequestApproval() throws Exception {
    when(config.canApprovePullRequest()).thenReturn(false);
    
    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(0);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);
    
    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(0);
    when(coverageReport.getEvolution()).thenReturn(10.0);
    
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq("INFO"), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
 
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(STASH_LOGIN), (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithCodeCoverageSecurityAsNone() throws Exception {
    when(stashRequestFacade.getCodeCoverageSeverity()).thenReturn(StashPlugin.SEVERITY_NONE);
    
    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);
    
    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(10);
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq("INFO"), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), anyString(), (SonarQubeClient) Mockito.anyObject());
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), (CoverageIssuesReport) Mockito.anyObject(), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), (CoverageIssuesReport) Mockito.anyObject(), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithCodeCoverageSecurityAsInfo() throws Exception {
    when(stashRequestFacade.getCodeCoverageSeverity()).thenReturn("INFO");

    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);

    CoverageIssuesReport coverageReport = mock(CoverageIssuesReport.class);
    when(coverageReport.countLoweredIssues()).thenReturn(10);
    when(stashRequestFacade.getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), eq("INFO"), (SonarQubeClient) Mockito.anyObject())).thenReturn(coverageReport);

    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);

    verify(stashRequestFacade, times(1)).getCoverageReport(eq(project.getKey()), eq(context), eq(inputFileCacheSensor), anyString(), (SonarQubeClient) Mockito.anyObject());

    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(coverageReport), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithoutAnalysisComment() throws Exception {
    when(config.includeAnalysisOverview()).thenReturn(Boolean.FALSE);

    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(101);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);

    int issueThreshold = 100;
    when(stashRequestFacade.getIssueThreshold()).thenReturn(issueThreshold);

    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade, inputFileCacheSensor);
    myJob.executeOn(project, context);

    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(ID_CARD), eq(STASH_ISSUE_THRESHOLD), eq(report), eq(coverageReport), (StashClient) Mockito.anyObject());
  }
}
