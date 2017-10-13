package org.sonar.plugins.stash;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Optional;

public class PeekableInputStream extends PushbackInputStream {
  public PeekableInputStream(InputStream in) {
    super(in);
  }

  public Optional<Character> peek() throws IOException {
    int next = read();
    if (next == -1) {
      return Optional.empty();
    } else {
      unread(next);
      return Optional.of(Character.valueOf((char)next));
    }
  }
}
