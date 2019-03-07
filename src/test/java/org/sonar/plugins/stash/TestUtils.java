package org.sonar.plugins.stash;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class TestUtils {
  private TestUtils() {}

  public static <T> T notNull(T t) {
    assertNotNull(t);
    return t;
  }

  public static void assertContains(String s, String expected) {
    assertNotNull(s);
    assertNotNull(expected);
    assertTrue(s.contains(expected));
  }

  // The first request to wiremock may be slow.
  // We could increase the timeout on our StashClient but then all the timeout test take longer.
  // So instead we perform a dummy request on each test invocation with a high timeout.
  // We now have many more request than before, but are faster anyways.
  public static void primeWireMock(WireMockServer wireMock) throws Exception {
    StubMapping primingMapping = WireMock.get(WireMock.urlPathEqualTo("/")).build();
    wireMock.addStubMapping(primingMapping);
    HttpURLConnection conn = (HttpURLConnection)new URL("http://127.0.0.1:" + wireMock.port()).openConnection();
    conn.setConnectTimeout(1000);
    conn.setConnectTimeout(1000);
    conn.connect();
    conn.getResponseCode();
    wireMock.removeStub(primingMapping);
    wireMock.resetRequests();
  }

  public static WrappedProcessBuilder createProcess(String prefix, String... command) {
    ProcessBuilder pb = new ProcessBuilder(command);
    return new WrappedProcessBuilder(prefix, pb);
  }

  public static class WrappedProcessBuilder {
    private final String prefix;
    private final ProcessBuilder wrapped;

    private WrappedProcessBuilder(String prefix, ProcessBuilder wrapped) {
      this.prefix = prefix;
      this.wrapped = wrapped;
    }

    public WrappedProcessBuilder directory(File directory) {
      wrapped.directory(directory);
      return this;
    }

    public Process start() throws IOException {
      return new WrappedProcess(prefix, wrapped.start());
    }

    public List<String> command() {
      return wrapped.command();
    }

    public Map<String, String> environment() {
        return wrapped.environment();
    }
  }

  public static class WrappedProcess extends Process {
    private final Process wrapped;
    private final ForwarderThread outputLogger;
    private final ForwarderThread errorLogger;

    private WrappedProcess(String prefix, Process wrapped) {
      this.wrapped = wrapped;
      errorLogger = new ForwarderThread(prefix, wrapped.getErrorStream());
      errorLogger.start();
      outputLogger = new ForwarderThread(prefix, wrapped.getInputStream());
      outputLogger.start();
    }

    @Override
    public OutputStream getOutputStream() {
      return wrapped.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
      return wrapped.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {
      return wrapped.getErrorStream();
    }

    @Override
    public int waitFor() throws InterruptedException {
      int exitCode = wrapped.waitFor();
      stopLoggers();
      return exitCode;
    }

    @Override
    public int exitValue() {
      return wrapped.exitValue();
    }

    @Override
    public void destroy() {
        wrapped.destroy();
    }

    private void stopLoggers() throws InterruptedException {
      outputLogger.interrupt();
      errorLogger.interrupt();
      outputLogger.join();
      errorLogger.join();
    }
  }

  private static class ForwarderThread extends Thread implements AutoCloseable {
    private final String prefix;
    private final InputStream input;

    private ForwarderThread(String prefix, InputStream input) {
      this.prefix = "[" + prefix + "] ";
      this.input = input;
      setDaemon(true);
    }

    @Override
    public void run() {
      try (BufferedReader lineReader = new BufferedReader(new InputStreamReader(input))) {
        while (!Thread.interrupted()) {
          String line = lineReader.readLine();
          if (line != null) {
            System.out.println(prefix + line);
            //logger.info(line);
          }
        }
      } catch (IOException e) {
          /* ignored */
      }
    }

    @Override
    public void close() throws InterruptedException {
        interrupt();
        join();
    }
  }
}
