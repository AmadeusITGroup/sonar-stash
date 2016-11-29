package org.sonar.plugins.stash.exceptions;

public class SonarQubeReportExtractionException extends StashException {

  public SonarQubeReportExtractionException(String message) {
    super(message);
  }

  public SonarQubeReportExtractionException(Throwable cause) {
    super(cause);
  }

  public SonarQubeReportExtractionException(String message, Throwable cause) {
    super(message, cause);
  }

}
