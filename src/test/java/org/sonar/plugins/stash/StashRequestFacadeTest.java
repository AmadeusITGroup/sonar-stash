package org.sonar.plugins.stash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.sonar.plugins.stash.client.StashCredentials;
import org.sonar.plugins.stash.exceptions.StashConfigurationException;

public class StashRequestFacadeTest {
    
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Mock
  private StashPluginConfiguration config;
  
  private StashRequestFacade facade;
  
  @Before
  public void setUp(){
    config = mock(StashPluginConfiguration.class); 
    facade = new StashRequestFacade(config);  
  }
    
  @Test
  public void testGetCredentials(){
    when(config.getStashLogin()).thenReturn("login");
    when(config.getStashPassword()).thenReturn("password");
    
    StashCredentials credentials = facade.getCredentials();
    assertEquals(credentials.getLogin(), "login");
    assertEquals(credentials.getPassword(), "password");
  }
  
  @Test
  public void testGetNoCredentials(){
    when(config.getStashLogin()).thenReturn(null);
    when(config.getStashPassword()).thenReturn(null);
    
    StashCredentials credentials = facade.getCredentials();
    assertNull(credentials.getLogin());
    assertNull(credentials.getPassword());
  }
  
  @Test
  public void testGetIssueThreshold() throws StashConfigurationException {
    when(config.getIssueThreshold()).thenReturn(1);
    assertEquals(facade.getIssueThreshold(), 1);
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetIssueThresholdThrowsException() throws StashConfigurationException {
    when(config.getIssueThreshold()).thenThrow(new NumberFormatException());
    facade.getIssueThreshold();
  }
 
  @Test
  public void testGetStashURL() throws StashConfigurationException {
    when(config.getStashURL()).thenReturn("http://url");
    assertEquals(facade.getStashURL(), "http://url");
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashURLThrowsException() throws StashConfigurationException {
    when(config.getStashURL()).thenReturn(null);
    facade.getStashURL();
  }
  
  @Test
  public void testGetStashProject() throws StashConfigurationException {
    when(config.getStashProject()).thenReturn("project");
    assertEquals(facade.getStashProject(), "project");
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashProjectThrowsException() throws StashConfigurationException {
    when(config.getStashProject()).thenReturn(null);
    facade.getStashProject();
  }
    
  @Test
  public void testGetStashRepository() throws StashConfigurationException {
    when(config.getStashRepository()).thenReturn("repository");
    assertEquals(facade.getStashRepository(), "repository");
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashRepositoryThrowsException() throws StashConfigurationException {
    when(config.getStashRepository()).thenReturn(null);
    facade.getStashRepository();
  }
 
  @Test
  public void testGetStashPullRequestId() throws StashConfigurationException {
    when(config.getPullRequestId()).thenReturn("12345");
    assertEquals(facade.getStashPullRequestId(), "12345");
  }
  
  @Test (expected = StashConfigurationException.class)
  public void testGetStashPullRequestIdThrowsException() throws StashConfigurationException {
    when(config.getPullRequestId()).thenReturn(null);
    facade.getStashPullRequestId();
  }
}
