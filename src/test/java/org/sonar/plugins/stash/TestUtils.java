package org.sonar.plugins.stash;

import static org.junit.Assert.assertNotNull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestUtils {
  private TestUtils() {}

  public static <T> T notNull(T t) {
    assertNotNull(t);
    return t;
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
}
