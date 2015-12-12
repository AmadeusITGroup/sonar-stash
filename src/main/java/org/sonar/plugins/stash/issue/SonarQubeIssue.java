package org.sonar.plugins.stash.issue;

public class SonarQubeIssue {

  private final String message;
  private final String severity;
  private final String rule;
  private final long line;
  private final String path;

  public SonarQubeIssue(String severity, String message, String rule, String path, long line) {
    this.message = message;
    this.severity = severity;
    this.rule = rule;
    this.path = path;
    this.line = line;
  }

  public String getSeverity() {
    return severity;
  }

  public String getMessage() {
    return message;
  }

  public String getRule() {
    return rule;
  }

  public long getLine() {
    return line;
  }

  public String getPath() {
    return path;
  }

}
