package org.sonar.plugins.stash;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public abstract class TestWatcherExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    starting();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    finished();
    if (context.getExecutionException().isPresent()) {
      failed();
    } else {
      succeeded();
    }
  }

  abstract void starting();
  abstract void finished();
  abstract void succeeded();
  abstract void failed();
}
