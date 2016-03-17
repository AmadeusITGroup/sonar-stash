package org.sonar.plugins.stash.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.sonar.plugins.stash.exceptions.SonarQubeClientException;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

public class SonarQubeClientTest {

  private static final String SONARQUBE_URL = "http://sonar";  
  private static final String PROJECT_KEY = "projectKey";
  
  @Spy
  SonarQubeClient myClient;
  
  @Mock
  BoundRequestBuilder requestBuilder;
  
  @Mock
  Response response;
  
  @Mock
  AsyncHttpClient httpClient;
  
  @Mock
  ListenableFuture<Response> listenableFurture;
  
  @Before
  public void setUp() throws Exception {
    requestBuilder = mock(BoundRequestBuilder.class);
    
    response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getStatusText()).thenReturn("Response status");
    
    listenableFurture = mock(ListenableFuture.class);
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(response);
    
    requestBuilder = mock(BoundRequestBuilder.class);
    when(requestBuilder.addHeader(anyString(), anyString())).thenReturn(requestBuilder);
    when(requestBuilder.execute()).thenReturn(listenableFurture);
    
    httpClient = mock(AsyncHttpClient.class);
    when(httpClient.prepareGet(anyString())).thenReturn(requestBuilder);
    doNothing().when(httpClient).close();
    
    SonarQubeClient client = new SonarQubeClient(SONARQUBE_URL);
    myClient = spy(client);
    doReturn(httpClient).when(myClient).createHttpClient();
  }
  
  
  @Test
  public void testGetCoveragePerProject() throws Exception {
    String sonarqubeJsonResponse = "[{\"msr\": [{\"key\":\"line_coverage\", \"val\": 60.0}]}]";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(sonarqubeJsonResponse);
    
    double result = myClient.getCoveragePerProject(PROJECT_KEY);
    
    assertTrue(result == 60.0);
    verify(httpClient, times(1)).close();
  }
  
  @Test
  public void testGetCoveragePerProjectWithUnknownKey() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);
    
    double result = myClient.getCoveragePerProject(PROJECT_KEY);
    
    assertTrue(result == 0);
    verify(httpClient, times(1)).close();
  }
  
  @Test
  public void testGetCoveragePerProjectWithException() throws Exception {
    String sonarqubeJsonResponse = "[{\"msr\": [{\"key\":\"line_coverage\", \"val\": 60.0}]}]";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(sonarqubeJsonResponse);
    
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      myClient.getCoveragePerProject(PROJECT_KEY);
      
      assertFalse("Exception failure should be catched and convert to SonarQubeClientException", true);
      
    } catch (SonarQubeClientException e) {
      verify(response, times(0)).getStatusText();
      verify(httpClient, times(1)).close();  
    }
  }
  
  @Test
  public void testGetCoveragePerFile() throws Exception {
    String sonarqubeJsonResponse = "[{\"msr\": [{\"key\":\"line_coverage\", \"val\": 60.0}]}]";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(sonarqubeJsonResponse);
    
    double result = myClient.getCoveragePerFile(PROJECT_KEY, "file1");
    
    assertTrue(result == 60.0);
    verify(httpClient, times(1)).close();
  }
  
  @Test
  public void testGetCoveragePerFileWithUnknownFile() throws Exception {
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_NO_CONTENT);
    
    double result = myClient.getCoveragePerFile(PROJECT_KEY, "file1");
    
    assertTrue(result == 0);
    verify(httpClient, times(1)).close();
  }
  
  @Test
  public void testGetCoveragePerFileWithException() throws Exception {
    String sonarqubeJsonResponse = "[{\"msr\": [{\"key\":\"line_coverage\", \"val\": 60.0}]}]";
    
    when(response.getStatusCode()).thenReturn(HttpURLConnection.HTTP_OK);
    when(response.getResponseBody()).thenReturn(sonarqubeJsonResponse);
    
    when(listenableFurture.get(anyLong(), eq(TimeUnit.MILLISECONDS))).thenThrow(new TimeoutException("TimeoutException for Test"));
    
    try {
      myClient.getCoveragePerFile(PROJECT_KEY, "file1");
      
      assertFalse("Exception failure should be catched and convert to SonarQubeClientException", true);
      
    } catch (SonarQubeClientException e) {
      verify(response, times(0)).getStatusText();
      verify(httpClient, times(1)).close();  
    }
  }  
}
