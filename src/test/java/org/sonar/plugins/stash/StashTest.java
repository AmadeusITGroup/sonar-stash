package org.sonar.plugins.stash;

import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class StashTest {
  @RegisterExtension
  public JavaUtilLoggingCapture logRule = new JavaUtilLoggingCapture();
}
