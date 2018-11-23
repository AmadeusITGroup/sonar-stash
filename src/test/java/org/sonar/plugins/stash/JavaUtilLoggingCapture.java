package org.sonar.plugins.stash;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;

public class JavaUtilLoggingCapture extends TestWatcherExtension {
  private boolean gobbleOnSuccess;
  private List<Handler> originalHandlers = new ArrayList<>();
  private Level originalLevel;
  private ConsoleHandler console = new ConsoleHandler();
  private MemoryHandler spool = new MemoryHandler(console, 10000, Level.OFF);
  private Logger logger;

  public JavaUtilLoggingCapture(boolean gobbleOnSuccess) {
    this.gobbleOnSuccess = gobbleOnSuccess;

    logger = Logger.getGlobal();
    Logger parent;
    while ((parent = logger.getParent()) != null) {
      logger = parent;
    }
  }

  public JavaUtilLoggingCapture() {
    this(true);
  }

  @Override
  protected void succeeded() {
    if (!gobbleOnSuccess) {
      emit();
    }
  }

  @Override
  protected void failed() {
    emit();
  }

  @Override
  protected void finished() {
    logger.removeHandler(spool);

    for (Handler h : originalHandlers) {
      logger.addHandler(h);
    }
    logger.setLevel(originalLevel);
  }

  @Override
  protected void starting() {
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
