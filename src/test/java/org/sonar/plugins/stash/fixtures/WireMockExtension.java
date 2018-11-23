package org.sonar.plugins.stash.fixtures;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import java.util.List;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// mostly copied from WireMockRule.java
public class WireMockExtension extends WireMockServer implements BeforeEachCallback,
    AfterEachCallback {

  private final boolean failOnUnmatchedRequests;

  public WireMockExtension(Options options) {
    this(options, true);
  }

  public WireMockExtension(Options options, boolean failOnUnmatchedRequests) {
    super(options);
    this.failOnUnmatchedRequests = failOnUnmatchedRequests;
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    start();
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    checkForUnmatchedRequests();
  }

  private void checkForUnmatchedRequests() {
    if (failOnUnmatchedRequests) {
      List<LoggedRequest> unmatchedRequests = findAllUnmatchedRequests();
      if (!unmatchedRequests.isEmpty()) {
        List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
        if (nearMisses.isEmpty()) {
          throw VerificationException.forUnmatchedRequests(unmatchedRequests);
        } else {
          throw VerificationException.forUnmatchedNearMisses(nearMisses);
        }
      }
    }
  }
}
