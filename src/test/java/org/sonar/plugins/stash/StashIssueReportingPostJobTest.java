package org.sonar.plugins.stash;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.resources.Project;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.issue.SonarQubeIssuesReport;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class StashIssueReportingPostJobTest {

  StashIssueReportingPostJob myJob;

  @Mock
  StashRequestFacade stashRequestFacade;

  @Mock
  StashPluginConfiguration config;

  @Mock
  ProjectIssues projectIssues;

  @Mock
  InputFileCache inputFileCache;

  @Mock
  Project project;

  @Mock
  SensorContext context;

  private static final String STASH_PROJECT = "Project";
  private static final String STASH_REPOSITORY = "Repository";
  private static final String STASH_PULLREQUEST_ID = "1";
  private static final String STASH_LOGIN = "login";
  private static final String STASH_PASSWORD = "password";
  private static final String STASH_URL = "http://url/to/stash";
  private static final int STASH_TIMEOUT = 10000;

  private static final String SONARQUBE_URL = "http://url/to/sonarqube";

  @Before
  public void setUp() throws Exception {
    projectIssues = mock(ProjectIssues.class);
    inputFileCache = mock(InputFileCache.class);

    project = new Project("1");
    context = mock(SensorContext.class);

    config = mock(StashPluginConfiguration.class);
    when(config.getStashURL()).thenReturn(STASH_URL);
    when(config.getSonarQubeURL()).thenReturn(SONARQUBE_URL);
    when(config.getStashTimeout()).thenReturn(STASH_TIMEOUT);

    stashRequestFacade = mock(StashRequestFacade.class);
    when(stashRequestFacade.getStashProject()).thenReturn(STASH_PROJECT);
    when(stashRequestFacade.getStashRepository()).thenReturn(STASH_REPOSITORY);
    when(stashRequestFacade.getStashPullRequestId()).thenReturn(STASH_PULLREQUEST_ID);
    when(stashRequestFacade.getCredentials()).thenReturn(new StashCredentials(STASH_LOGIN, STASH_PASSWORD));
  }

  @Test
  public void testExecuteOn() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(true);

    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);

    int issueThreshold = 100;
    when(stashRequestFacade.getIssueThreshold()).thenReturn(issueThreshold);

    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade);
    myJob.executeOn(project, context);

    verify(stashRequestFacade, times(1)).postCommentPerIssue(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report),
      (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(issueThreshold), eq(report),
      (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithReachedThreshold() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(true);

    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(101);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);

    int issueThreshold = 100;
    when(stashRequestFacade.getIssueThreshold()).thenReturn(issueThreshold);

    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade);
    myJob.executeOn(project, context);

    verify(stashRequestFacade, times(0)).postCommentPerIssue(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report),
      (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1)).postAnalysisOverview(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(issueThreshold), eq(report),
      (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithNoPluginActivation() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(false);

    SonarQubeIssuesReport report = mock(SonarQubeIssuesReport.class);
    when(report.countIssues()).thenReturn(10);
    when(stashRequestFacade.extractIssueReport(projectIssues, inputFileCache)).thenReturn(report);

    int issueThreshold = 100;
    when(stashRequestFacade.getIssueThreshold()).thenReturn(issueThreshold);

    myJob = new StashIssueReportingPostJob(config, projectIssues, inputFileCache, stashRequestFacade);
    myJob.executeOn(project, context);

    verify(stashRequestFacade, times(0)).postCommentPerIssue(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(report),
      (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0)).postAnalysisOverview(eq(STASH_PROJECT), eq(STASH_REPOSITORY), eq(STASH_PULLREQUEST_ID), eq(SONARQUBE_URL), eq(issueThreshold), eq(report),
      (StashClient) Mockito.anyObject());
  }
}
