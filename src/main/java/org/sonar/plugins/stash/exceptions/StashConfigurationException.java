package org.sonar.plugins.stash.exceptions;

public class StashConfigurationException extends StashException {

  private static final long serialVersionUID = 8423412434061160213L;

  public StashConfigurationException(String message) {
    super(message);
  }

  public StashConfigurationException(Throwable cause) {
    super(cause);
  }

  public StashConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }

}
