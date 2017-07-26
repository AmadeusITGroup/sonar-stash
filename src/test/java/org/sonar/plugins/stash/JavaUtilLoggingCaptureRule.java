package org.sonar.plugins.stash;

import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;

public class JavaUtilLoggingCaptureRule extends TestWatcher implements TestRule {
  private boolean gobbleOnSuccess;
  private List<Handler> originalHandlers = new ArrayList<>();
  private Level originalLevel;
  private ConsoleHandler console = new ConsoleHandler();
  private MemoryHandler spool = new MemoryHandler(console, 10000, Level.OFF);
  private Logger logger;

  public JavaUtilLoggingCaptureRule(boolean gobbleOnSuccess) {
    this.gobbleOnSuccess = gobbleOnSuccess;

    logger = Logger.getGlobal();
    Logger parent;
    while ((parent = logger.getParent()) != null) {
      logger = parent;
    }
  }

  public JavaUtilLoggingCaptureRule() {
    this(true);
  }

  @Override
  protected void succeeded(Description description) {
    if (!gobbleOnSuccess) {
      emit();
    }
  }

  @Override
  protected void failed(Throwable t, Description description) {
    emit();
  }

  @Override
  protected void finished(Description description) {
    logger.removeHandler(spool);

    for (Handler h : originalHandlers) {
      logger.addHandler(h);
    }
    logger.setLevel(originalLevel);
  }

  @Override
  protected void starting(Description description) {
    for (Handler h : logger.getHandlers()) {
      originalHandlers.add(h);
      logger.removeHandler(h);
    }
    logger.addHandler(spool);

    originalLevel = logger.getLevel();
    logger.setLevel(Level.ALL);
  }

  private void emit() {
    console.publish(new LogRecord(Level.SEVERE, "Replaying captured log entries"));
    spool.push();
    spool.flush();
  }
}
