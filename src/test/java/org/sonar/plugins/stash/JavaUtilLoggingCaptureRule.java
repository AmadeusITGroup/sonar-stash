package org.sonar.plugins.stash;

import org.junit.rules.*;
import org.junit.runner.*;
import org.junit.runners.model.*;

import java.util.*;
import java.util.logging.*;

public class JavaUtilLoggingCaptureRule extends TestWatcher implements TestRule {
    private boolean gobbleOnSuccess;
    private List<Handler> originalHandlers = new ArrayList<>();
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
    }

    @Override
    protected void starting(Description description) {
        for (Handler h: logger.getHandlers()) {
            originalHandlers.add(h);
            logger.removeHandler(h);
        }
        logger.addHandler(spool);
    }

    private void emit() {
        console.publish(new LogRecord(Level.SEVERE, "Replaying captured log entries"));
        spool.push();
        spool.flush();
    }
}
