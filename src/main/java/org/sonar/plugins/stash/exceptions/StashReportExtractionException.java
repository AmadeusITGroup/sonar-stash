package org.sonar.plugins.stash.exceptions;

public class StashReportExtractionException extends StashException {

  private static final long serialVersionUID = -8105232303389875137L;

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
