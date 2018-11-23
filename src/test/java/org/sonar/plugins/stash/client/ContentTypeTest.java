package org.sonar.plugins.stash.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ContentTypeTest {

  @Test
  public void testContentType() {
    ContentType json = new ContentType("application", "json", null);
    assertTrue(json.match("application/json"));
    assertTrue(json.match("application/json;charset=utf-8"));
    assertTrue(json.match("APPLICATION/JSON"));
    assertTrue(json.match("ApPlIcAtIoN/jSoN"));

    assertFalse(json.match(""));
    assertFalse(json.match("/"));
    assertFalse(json.match(";"));
    assertFalse(json.match("/;"));
    assertFalse(json.match("foo"));
    assertFalse(json.match("12!4"));
    assertFalse(json.match("foo/json"));
    assertFalse(json.match("application/foo"));
    assertFalse(json.match("application/foo;charset=utf-8"));
  }

  @Test
  public void testConstructorFailure() {
    assertThrows(IllegalArgumentException.class, () ->
        new ContentType("application", "json", "what-is-the-point-if-I-must-be-null?")
    );
  }
}