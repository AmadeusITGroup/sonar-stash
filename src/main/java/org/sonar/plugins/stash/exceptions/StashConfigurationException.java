package org.sonar.plugins.stash.exceptions;

public class StashConfigurationException extends StashException {

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
