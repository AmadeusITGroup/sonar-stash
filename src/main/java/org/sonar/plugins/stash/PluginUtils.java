package org.sonar.plugins.stash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public final class PluginUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);

  private static final String ERROR_DETAILS = "Exception detected: {}";
  private static final String ERROR_STACK = "Exception stack trace";

  private PluginUtils() {
    // Hiding implicit public constructor with an explicit private one (squid:S1118)
  }

  public static PluginInfo infoForPluginClass(Class klass) {
    try {
      Enumeration<URL> resources = klass.getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        URL resource = resources.nextElement();
        Manifest man;
        try (InputStream ms = resource.openStream()) {
          man = new Manifest(ms);
        }

        Attributes attrs = man.getMainAttributes();
        if (attrs == null) {
          continue;
        }

        String pluginClass = attrs.getValue("Plugin-Class");

        if (!klass.getName().equals(pluginClass)) {
          continue;
        }

        String pluginVersion = attrs.getValue("Plugin-Version");
        String pluginName = attrs.getValue("Plugin-Name");
        return new PluginInfo(pluginName, pluginVersion);
      }
    } catch (IOException e) {
      LOGGER.warn(ERROR_DETAILS, e.getMessage());
      LOGGER.debug(ERROR_STACK, e);
      return null;
    }

    return null;
  }
}
