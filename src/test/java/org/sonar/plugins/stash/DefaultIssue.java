package org.sonar.plugins.stash;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;

public class DefaultIssue implements PostJobIssue {
  private String key, componentKey, message;
  private InputComponent inputComponent;
  private boolean isNew;
  private Severity severity;
  private Integer line;
  private RuleKey ruleKey;

  public String key() {
    return key;
  }

  public DefaultIssue setKey(String key) {
    this.key = key;
    return this;
  }

  public String componentKey() {
    return componentKey;
  }

  public DefaultIssue setComponentKey(String componentKey) {
    this.componentKey = componentKey;
    return this;
  }

  public String message() {
    return message;
  }

  public DefaultIssue setMessage(String message) {
    this.message = message;
    return this;
  }

  public InputComponent inputComponent() {
    return inputComponent;
  }

  public DefaultIssue setInputComponent(InputComponent inputComponent) {
    this.inputComponent = inputComponent;
    return this;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  public DefaultIssue setNew(boolean aNew) {
    isNew = aNew;
    return this;
  }

  public Severity severity() {
    return severity;
  }

  public DefaultIssue setSeverity(Severity severity) {
    this.severity = severity;
    return this;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultIssue setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public DefaultIssue setLine(Integer line) {
    this.line = line;
    return this;
  }

  @Override
  public Integer line() {
    return line;
  }
}