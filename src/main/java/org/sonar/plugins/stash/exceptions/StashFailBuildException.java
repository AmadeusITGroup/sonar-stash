package org.sonar.plugins.stash.exceptions;

public class StashFailBuildException extends RuntimeException {
  
  public StashFailBuildException(String message) {
    super(message);
  }

  
}
