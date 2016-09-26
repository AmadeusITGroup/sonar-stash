package org.sonar.plugins.stash;

import org.junit.Assert;
import org.junit.Test;

public class ContentTypeTest {
    @Test
    public void testContentType() {
        ContentType json = new ContentType("application", "json", null);
        Assert.assertTrue(json.match("application/json"));
        Assert.assertTrue(json.match("application/json;charset=utf-8"));
        Assert.assertTrue(json.match("APPLICATION/JSON"));
        Assert.assertTrue(json.match("ApPlIcAtIoN/jSoN"));

        Assert.assertFalse(json.match(""));
        Assert.assertFalse(json.match("/"));
        Assert.assertFalse(json.match(";"));
        Assert.assertFalse(json.match("/;"));
        Assert.assertFalse(json.match("foo"));
        Assert.assertFalse(json.match("12!4"));
        Assert.assertFalse(json.match("foo/json"));
        Assert.assertFalse(json.match("application/foo"));
        Assert.assertFalse(json.match("application/foo;charset=utf-8"));
    }
}
