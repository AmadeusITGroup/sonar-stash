package org.sonar.plugins.stash;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.coverage.CoverageProjectStore;
import org.sonar.plugins.stash.issue.StashDiffReport;
import org.sonar.plugins.stash.issue.StashUser;


@RunWith(MockitoJUnitRunner.class)
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

  Project project = new Project("1");
  
  @Mock
  StashDiffReport diffReport;
  
  @Mock
  List<Issue> report;

  @Mock
  SensorContext context;

  @Mock
  CoverageProjectStore coverageProjectStore;

  private static final String STASH_PROJECT = "Project";
  private static final String STASH_REPOSITORY = "Repository";
  private static final int STASH_PULLREQUEST_ID = 1;
  private static final String STASH_LOGIN = "login";
  private static final String STASH_PASSWORD = "password";
  private static final String STASH_URL = "http://url/to/stash";
  private static final int STASH_TIMEOUT = 10000;
  private static final int STASH_ISSUE_THRESHOLD = 100;
  private static final PullRequestRef pr = PullRequestRef.builder()
          .setProject(STASH_PROJECT)
          .setRepository(STASH_REPOSITORY)
          .setPullRequestId(STASH_PULLREQUEST_ID)
          .build();

  
  private static final String SONARQUBE_URL = "http://url/to/sonarqube";

  @Before
  public void setUp() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(true);
    when(config.canApprovePullRequest()).thenReturn(false);
    when(config.getStashURL()).thenReturn(STASH_URL);
    when(config.getSonarQubeURL()).thenReturn(SONARQUBE_URL);
    when(config.getStashTimeout()).thenReturn(STASH_TIMEOUT);
    when(config.resetComments()).thenReturn(false);
    when(config.hasToNotifyStash()).thenReturn(true);
    when(config.includeAnalysisOverview()).thenReturn(Boolean.TRUE);

    when(report.size()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues)).thenReturn(report);

    when(coverageProjectStore.getProjectCoverage()).thenReturn(20.0);
    when(coverageProjectStore.getPreviousProjectCoverage()).thenReturn(10.0);
    // when(coverageReport.countLoweredIssues()).thenReturn(5);

    when(stashRequestFacade.getIssueThreshold()).thenReturn(STASH_ISSUE_THRESHOLD);
    when(stashRequestFacade.getStashProject()).thenReturn(STASH_PROJECT);
    when(stashRequestFacade.getStashRepository()).thenReturn(STASH_REPOSITORY);
    when(stashRequestFacade.getCredentials()).thenReturn(new StashCredentials(STASH_LOGIN, STASH_PASSWORD));
    when(stashRequestFacade.getSonarQubeReviewer(Mockito.anyString(), (StashClient) Mockito.anyObject())).thenReturn(stashUser);
    when(stashRequestFacade.getPullRequestDiffReport(eq(pr), (StashClient) Mockito.anyObject())).thenReturn(diffReport);
    when(stashRequestFacade.getIssueThreshold()).thenReturn(STASH_ISSUE_THRESHOLD);
    when(stashRequestFacade.getStashURL()).thenReturn(STASH_URL);

    when(stashRequestFacade.getPullRequest(any())).thenReturn(pr);
  }
  
  @Test
  public void testExecuteOn() throws Exception {
    myJob = new StashIssueReportingPostJob(config, projectIssues, stashRequestFacade);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithReachedThreshold() throws Exception {
    when(stashRequestFacade.getIssueThreshold()).thenReturn(10);
    
    List<Issue> report = spy(new ArrayList<Issue>());
    when(report.size()).thenReturn(55);
    when(stashRequestFacade.extractIssueReport(projectIssues)).thenReturn(report);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, stashRequestFacade);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(10), eq(report), (StashClient) Mockito.anyObject());
   }
  
  @Test
  public void testExecuteOnWithNoPluginActivation() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(false);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, stashRequestFacade);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
 }
  
  @Test
  public void testExecuteOnWithNoStashUserDefined() throws Exception {
    when(stashRequestFacade.getSonarQubeReviewer(Mockito.anyString(), (StashClient) Mockito.anyObject())).thenReturn(null);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, stashRequestFacade);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
 }
  
  @Test
  public void testExecuteOnWithResetCommentActivated() throws Exception {
    when(config.resetComments()).thenReturn(true);

    myJob = new StashIssueReportingPostJob(config, projectIssues, stashRequestFacade);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(1)).resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
  }
  
  @Test
  public void testExecuteOnWithNoDiffReport() throws Exception {
    diffReport = null;
    when(stashRequestFacade.getPullRequestDiffReport(eq(pr), (StashClient) Mockito.anyObject())).thenReturn(diffReport);
    
    myJob = new StashIssueReportingPostJob(config, projectIssues, stashRequestFacade);
    myJob.executeOn(project, context);
    
    verify(stashRequestFacade, times(0)).resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }

  /* FIXME
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
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(pr), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }
  */

  /*
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
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(pr), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }
  */
  

  /*
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
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(pr), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }
  */

  /*
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
 
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(pr), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
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
    
    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(pr), (CoverageIssuesReport) Mockito.anyObject(), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (CoverageIssuesReport) Mockito.anyObject(), (StashClient) Mockito.anyObject());
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

    verify(stashRequestFacade, times(1)).postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postCoverageReport(eq(pr), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
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

    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(pr), eq(STASH_ISSUE_THRESHOLD), eq(report), (StashClient) Mockito.anyObject());
  }
  */
}
