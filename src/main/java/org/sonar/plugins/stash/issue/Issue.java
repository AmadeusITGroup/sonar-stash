package org.sonar.plugins.stash.issue;

public abstract class Issue {

  protected final String key;
  protected final String severity;
  protected final long line;
  protected final String path;

  public Issue(String key, String severity, String path, long line) {
    this.key = key;
    this.severity = severity;
    this.path = path;
    this.line = line;
  }

  public String getKey() {
    return key;
  }

  public String getSeverity() {
    return severity;
  }

  public long getLine() {
    return line;
  }

  public String getPath() {
    return path;
  }
  
  public abstract String getMessage();
  
  public abstract String printIssueMarkdown(String sonarQubeURL);
  
}
