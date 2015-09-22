package org.sonar.plugins.stash.exceptions;

public class StashException extends Exception {

  public StashException(String message) {
    super(message);
  }

  public StashException(Throwable cause) {
    super(cause);
  }
  
  public StashException(String message, Throwable cause) {
    super(message, cause);
  }
  
}
