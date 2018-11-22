package org.sonar.plugins.stash.fixtures;

import static org.junit.Assert.assertEquals;

import com.google.common.base.Splitter;
import java.nio.file.Path;
import javax.annotation.CheckForNull;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.rule.RuleKey;

public class DummyPostJobIssue implements PostJobIssue {
  private final String key;
  private final RuleKey ruleKey;
  private final String componentKey;
  private final Integer line;
  private final String message;
  private final Severity severity;
  private final InputComponent component;

  public DummyPostJobIssue(Path moduleBaseDir, String key, RuleKey ruleKey, String module, String relativePath, Integer line,
      String message, Severity severity) {
    this.key = key;
    this.ruleKey = ruleKey;
    this.componentKey = module + ":" + relativePath;
    this.line = line;
    this.message = message;
    this.severity = severity;
    component = new DefaultInputFile(componentKey, relativePath).setModuleBaseDir(moduleBaseDir);
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  @Override
  public String componentKey() {
    return componentKey;
  }

  @CheckForNull
  @Override
  public InputComponent inputComponent() {
    return component;
  }

  @CheckForNull
  @Override
  public Integer line() {
    return line;
  }

  @CheckForNull
  @Override
  public String message() {
    return message;
  }

  @Override
  public Severity severity() {
    return severity;
  }

  @Override
  public boolean isNew() {
    return true;
  }
}
