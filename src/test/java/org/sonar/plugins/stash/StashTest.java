package org.sonar.plugins.stash;

import org.junit.*;

public abstract class StashTest {
    @Rule
    public JavaUtilLoggingCaptureRule logRule = new JavaUtilLoggingCaptureRule();
}
