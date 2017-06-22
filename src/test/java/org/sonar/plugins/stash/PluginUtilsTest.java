package org.sonar.plugins.stash;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;


public class PluginUtilsTest {

  @Test
  public void testConstructorIsPrivate() throws Exception {

    // Let's use this for the greater good: we make sure that nobody can create an instance of this class
    Constructor constructor = PluginUtils.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(constructor.getModifiers()));

    // This part is for code coverage only (but is re-using the elments above... -_^)
    constructor.setAccessible(true);
    constructor.newInstance();
  }
}