package org.sonar.plugins.stash.fixtures;

import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.issue.PostJobIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;

import java.util.Collection;
import java.util.Collections;

public class DummyPostJobContext implements PostJobContext {
  private final MapSettings settings;
  private final AnalysisMode mode;
  private final Collection<PostJobIssue> issues;

  public DummyPostJobContext(
      MapSettings settings, AnalysisMode mode, Collection<PostJobIssue> issues) {
    this.settings = settings;
    this.mode = mode;
    this.issues = issues;
  }

  /** @deprecated since 6.5 use {@link #config()} */
  @Override
  public MapSettings settings() {
    return settings;
  }

  /**
   * Get configuration of the current project.
   *
   * @since 6.5
   */
  @Override
  public Configuration config() {
    return settings.asConfig();
  }

  /** @deprecated since 7.3 preview mode deprecated since 6.6 */
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
