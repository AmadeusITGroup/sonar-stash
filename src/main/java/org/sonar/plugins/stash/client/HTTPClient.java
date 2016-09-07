package org.sonar.plugins.stash.client;

public interface HTTPClient extends AutoCloseable {
    public void close();
}
