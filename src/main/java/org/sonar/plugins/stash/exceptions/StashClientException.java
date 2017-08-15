package org.sonar.plugins.stash.exceptions;

public class StashClientException extends StashException {

  private static final long serialVersionUID = -9071818008552452736L;

  public StashClientException(String message) {
    super(message);
  }

  public StashClientException(Throwable cause) {
    super(cause);
  }

  public StashClientException(String message, Throwable cause) {
    super(message, cause);
  }

}
