package org.sonar.plugins.stash.issue;

public class SonarQubeIssue extends Issue {

  private final String rule;
  private final String message;
  
  public SonarQubeIssue(String key, String severity, String message, String rule, String path, long line) {
    super(key, severity, path, line);
    this.rule = rule;
    this.message = message;
  }

  public String getRule() {
    return rule;
  }
  
  public String getMessage() {
    return message;
  }
    
  public String printIssueMarkdown(String sonarQubeURL) {
    StringBuilder sb = new StringBuilder();
    sb.append(MarkdownPrinter.printSeverityMarkdown(severity)).append(getMessage()).append(" [[").append(rule)
        .append("]").append("(").append(sonarQubeURL).append("/").append(MarkdownPrinter.CODING_RULES_RULE_KEY).append(rule).append(")]");

    return sb.toString();
  }

}
