package org.sonar.plugins.stash;

import org.junit.Rule;

public abstract class StashTest {
  @Rule
  public JavaUtilLoggingCaptureRule logRule = new JavaUtilLoggingCaptureRule();
}
