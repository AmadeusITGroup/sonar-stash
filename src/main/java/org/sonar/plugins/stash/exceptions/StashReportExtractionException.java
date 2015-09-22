package org.sonar.plugins.stash.exceptions;

public class StashReportExtractionException extends StashException {

  public StashReportExtractionException(String message) {
    super(message);
  }

  public StashReportExtractionException(Throwable cause) {
    super(cause);
  }

  public StashReportExtractionException(String message, Throwable cause) {
    super(message, cause);
  }

}
