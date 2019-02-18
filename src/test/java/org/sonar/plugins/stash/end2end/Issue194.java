package org.sonar.plugins.stash.end2end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.stash.StashPlugin;

public class Issue194 extends EndToEndTest {
  @Test
  public void testWithVicinitiyRange() throws Exception {
    run(0);
  }

  @Test
  public void testAfterVicinityRange() throws Exception {
    run(5);
  }

  private void run(int vicinityRange) throws Exception {
    RunResults results = run("issue194_stash_diff",
        (settings) -> settings.setProperty(StashPlugin.STASH_INCLUDE_VICINITY_RANGE, vicinityRange),
        Arrays.asList(
            issue("01673981387EDCCDB0", "ModifiersOrderCheck", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 56),
            issue("01673981387EDCCDB1", "S106", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 109),
            issue("01673981387EDCCDB2", "S106", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 120),
            issue("01673981387EDCCDB3", "S2583", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 108),
            issue("01673981387EDCCDB4", "S1481", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 106),
            issue("01673981387EDCCDB5", "S1185", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 207),
            issue("01673981387EDCCDB6", "S00117", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 106),
            issue("01673981387EDCCDB7", "S00117", "com.gdn.package:module-api-client", "aModule-api-client", "src/main/java/com/gdn/package/aModule/client/aModuleClient.java", 119)
        ));
    results.comments = results.comments.stream().filter(o -> o.has("anchor")).collect(Collectors.toList());
    assertEquals(6, results.comments.size());
    Optional<JsonObject> comment1 = results.comments.stream().filter((c) -> c.get("text").getAsString().contains("S1481")).findFirst();
    assertTrue(comment1.isPresent());
    comment1.ifPresent(c -> {
      assertNotNull(c);
      assertEquals(106, c.get("anchor").getAsJsonObject().get("line").getAsLong());
    });
    for (JsonObject o: results.comments) {
      assertEquals("TO", o.get("anchor").getAsJsonObject().get("fileType").getAsString());
    }
  }
}