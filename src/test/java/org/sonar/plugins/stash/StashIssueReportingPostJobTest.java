package org.sonar.plugins.stash;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.platform.Server;
import org.sonar.plugins.stash.client.StashClient;
import org.sonar.plugins.stash.client.StashCredentials;
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
  StashDiffReport diffReport;

  @Mock
  List<PostJobIssue> report;

  @Mock
  PostJobContext context;

  @Mock
  Server server;

  private static final String STASH_PROJECT = "Project";
  private static final String STASH_REPOSITORY = "Repository";
  private static final int STASH_PULLREQUEST_ID = 1;
  private static final String STASH_LOGIN = "login@email.com";
  private static final String STASH_USER_SLUG = "login";
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
    when(stashRequestFacade.extractIssueReport(eq(report)))
        .thenReturn(report);
    when(context.issues()).thenReturn(report);

    when(stashRequestFacade.getIssueThreshold()).thenReturn(STASH_ISSUE_THRESHOLD);
    when(stashRequestFacade.getStashProject()).thenReturn(STASH_PROJECT);
    when(stashRequestFacade.getStashRepository()).thenReturn(STASH_REPOSITORY);
    when(stashRequestFacade.getStashPullRequestId()).thenReturn(STASH_PULLREQUEST_ID);
    when(stashRequestFacade.getCredentials()).thenReturn(new StashCredentials(STASH_LOGIN,
        STASH_PASSWORD,
        STASH_USER_SLUG));
    when(stashRequestFacade
        .getSonarQubeReviewer(Mockito.anyString(), (StashClient) Mockito.anyObject())).thenReturn(
        stashUser);
    when(stashRequestFacade.getPullRequestDiffReport(eq(pr), (StashClient) Mockito.anyObject()))
        .thenReturn(diffReport);
    when(stashRequestFacade.getIssueThreshold()).thenReturn(STASH_ISSUE_THRESHOLD);
    when(stashRequestFacade.getStashURL()).thenReturn(STASH_URL);

    when(stashRequestFacade.getPullRequest()).thenReturn(pr);
  }

  @Test
  public void testExecuteOn() throws Exception {
    myJob = new StashIssueReportingPostJob(config, stashRequestFacade, server);
    myJob.execute(context);

    verify(stashRequestFacade, times(0))
        .resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1))
        .postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1))
        .postAnalysisOverview(eq(pr), eq(report),
            (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithReachedThreshold() throws Exception {
    when(stashRequestFacade.getIssueThreshold()).thenReturn(10);

    List<PostJobIssue> report = mock(ArrayList.class);
    when(report.size()).thenReturn(55);
    when(stashRequestFacade.extractIssueReport(eq(report)))
        .thenReturn(report);

    myJob = new StashIssueReportingPostJob(config, stashRequestFacade, server);
    myJob.execute(context);

    verify(stashRequestFacade, times(0))
        .resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1))
        .postAnalysisOverview(eq(pr), Mockito.any(Collection.class), (StashClient) Mockito.anyObject()
        );
  }

  @Test
  public void testExecuteOnWithNoPluginActivation() throws Exception {
    when(config.hasToNotifyStash()).thenReturn(false);

    myJob = new StashIssueReportingPostJob(config, stashRequestFacade, server);
    myJob.execute(context);

    verify(stashRequestFacade, times(0))
        .resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postAnalysisOverview(eq(pr), eq(report),
            (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithNoStashUserDefined() throws Exception {
    when(stashRequestFacade.getSonarQubeReviewer(Mockito.anyString(),
        (StashClient) Mockito.anyObject())).thenReturn(null);

    myJob = new StashIssueReportingPostJob(config, stashRequestFacade, server);
    myJob.execute(context);

    verify(stashRequestFacade, times(0))
        .resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postAnalysisOverview(eq(pr), eq(report),
            (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithResetCommentActivated() throws Exception {
    when(config.resetComments()).thenReturn(true);

    myJob = new StashIssueReportingPostJob(config, stashRequestFacade, server);
    myJob.execute(context);

    verify(stashRequestFacade, times(1))
        .resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1))
        .postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(1))
        .postAnalysisOverview(eq(pr), eq(report),
            (StashClient) Mockito.anyObject());
  }

  @Test
  public void testExecuteOnWithNoDiffReport() throws Exception {
    diffReport = null;
    when(stashRequestFacade.getPullRequestDiffReport(eq(pr), (StashClient) Mockito.anyObject()))
        .thenReturn(diffReport);

    myJob = new StashIssueReportingPostJob(config, stashRequestFacade, server);
    myJob.execute(context);

    verify(stashRequestFacade, times(0))
        .resetComments(eq(pr), eq(diffReport), eq(stashUser), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postSonarQubeReport(eq(pr), eq(report), eq(diffReport), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .postAnalysisOverview(eq(pr), eq(report),
            (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .approvePullRequest(eq(pr), (StashClient) Mockito.anyObject());
    verify(stashRequestFacade, times(0))
        .resetPullRequestApproval(eq(pr), (StashClient) Mockito.anyObject());
  }

  @Test
  public void testShouldApprovePullRequest() {
    PostJobIssue minorIssue = new DefaultIssue().setSeverity(Severity.MINOR);
    PostJobIssue majorIssue = new DefaultIssue().setSeverity(Severity.MAJOR);

    report = new ArrayList<>();

    report.add(minorIssue);
    report.add(majorIssue);

    assertFalse(
        StashIssueReportingPostJob.shouldApprovePullRequest(Optional.empty(), report)
    );

    report.clear();
    assertTrue(
        StashIssueReportingPostJob.shouldApprovePullRequest(Optional.empty(), report)
    );

    report.add(minorIssue);
    assertTrue(
        StashIssueReportingPostJob.shouldApprovePullRequest(Optional.of(Severity.MINOR), report)
    );

    report.add(majorIssue);
    assertFalse(
        StashIssueReportingPostJob.shouldApprovePullRequest(Optional.of(Severity.MINOR), report)
    );
  }
}
