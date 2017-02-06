package org.sonar.plugins.stash.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.stash.exceptions.SonarQubeClientException;
import org.sonar.plugins.stash.exceptions.SonarQubeReportExtractionException;
import org.sonar.plugins.stash.issue.collector.SonarQubeCollector;

public class SonarQubeClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(SonarQubeClient.class);
  
  private static final int SONAR_TIMEOUT = 1000;
  
  static final String REST_API = "/api/";
  static final String RESOURCE_API = REST_API + "resources"; 
  static final String COVERAGE_RESOURCE_API = "{0}" + RESOURCE_API + "?resource={1}&metrics=line_coverage&format=json"; 
  
  private final String sonarHostUrl;
   
  public SonarQubeClient(String sonarHostUrl) {
    this.sonarHostUrl = sonarHostUrl;
  }

  public String getBaseUrl() {
      return this.sonarHostUrl;
  }

  public double getCoveragePerProject(String projectKey) throws SonarQubeClientException {
    return getCoverage(projectKey);
  }
  
  public double getCoveragePerFile(String projectKey, String filePath) throws SonarQubeClientException {
    return getCoverage(projectKey + ":" + filePath);
  }
 
  private double getCoverage(String key) throws SonarQubeClientException {
    double result = 0;
    AsyncHttpClient httpClient = createHttpClient();
    
    try {
      String request = MessageFormat.format(COVERAGE_RESOURCE_API, sonarHostUrl, key);
      BoundRequestBuilder requestBuilder = httpClient.prepareGet(request);
  
      Response response = executeRequest(requestBuilder);
      int responseCode = response.getStatusCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        LOGGER.debug("Unable to get the coverage on resource " + key + ": " + response.getStatusText() + "(" + responseCode + ")" );
      } else {
        String jsonBody = response.getResponseBody();
        result = SonarQubeCollector.extractCoverage(jsonBody);
      }
    } catch (ExecutionException | TimeoutException | InterruptedException | IOException | SonarQubeReportExtractionException e) {
      throw new SonarQubeClientException("Unable to get the coverage on resource " + key, e);
    } finally {
      try {
        httpClient.close();
      } catch (IOException ignored) {
          /* ignore */
      }
    }
    
    return result;
  }
  
  Response executeRequest(final BoundRequestBuilder requestBuilder) throws InterruptedException, IOException, ExecutionException, TimeoutException {

    requestBuilder.addHeader("Content-Type", "application/json");
    return requestBuilder.execute().get(SONAR_TIMEOUT, TimeUnit.MILLISECONDS);
  }

  AsyncHttpClient createHttpClient(){
    return new DefaultAsyncHttpClient();
  }
}
