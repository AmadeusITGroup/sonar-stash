package org.sonar.plugins.stash.fixtures;

import java.util.Collection;
import java.util.Collections;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.ConfigurationBridge;

public class DummyPostJobContext implements PostJobContext {
  private final Settings settings;
  private final AnalysisMode mode;
  private final Collection<PostJobIssue> issues;

  public DummyPostJobContext(Settings settings, AnalysisMode mode,
      Collection<PostJobIssue> issues) {
    this.settings = settings;
    this.mode = mode;
    this.issues = issues;
  }

  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public Configuration config() {
    return new ConfigurationBridge(settings);
  }

  @Override
  public AnalysisMode analysisMode() {
    return mode;
  }

  @Override
  public Iterable<PostJobIssue> issues() {
    return issues;
  }

  @Override
  public Iterable<PostJobIssue> resolvedIssues() {
    return Collections.emptyList();
  }
}
