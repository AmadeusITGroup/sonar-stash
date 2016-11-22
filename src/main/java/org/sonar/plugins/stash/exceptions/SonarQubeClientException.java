package org.sonar.plugins.stash.exceptions;

public class SonarQubeClientException extends Exception {

  public SonarQubeClientException(String message) {
    super(message);
  }
  
  public SonarQubeClientException(String message, Exception e) {
    super(message, e);
  }
  
}
