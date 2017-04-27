package org.sonar.plugins.stash;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class PeekableInputStreamTest {
    @Test
    public void testEmptyStream() throws IOException {
        PeekableInputStream s = fromString("");

        assertFalse(s.peek().isPresent());
        assertFalse(s.peek().isPresent());
        assertFalse(s.peek().isPresent());
    }

    @Test
    public void testStream() throws IOException {
        PeekableInputStream s = fromString("abcde");

        assertEquals((Character) 'a', s.peek().get());
        assertEquals((Character) 'a', s.peek().get());
        assertEquals((Character) 'a', s.peek().get());
    }

    private PeekableInputStream fromString(String s) {
        return new PeekableInputStream(new StringBufferInputStream(s));
    }
}
