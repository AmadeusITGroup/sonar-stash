package org.sonar.plugins.stash.exceptions;

public class GitBranchNotFoundOrNotUniqueException extends RuntimeException {

  public GitBranchNotFoundOrNotUniqueException(String message) {
    super(message);
  }
  
}
