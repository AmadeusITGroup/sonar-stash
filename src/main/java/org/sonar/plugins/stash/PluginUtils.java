package org.sonar.plugins.stash;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginUtils {

    // Hiding implicit public constructor with an explicit private one (squid:S1118)
    private PluginUtils() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtils.class);


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
                if (pluginClass == null) {
                    continue;
                }
                if (!pluginClass.equals(klass.getName())) {
                    continue;
                }

                String pluginVersion = attrs.getValue("Plugin-Version");
                String pluginName = attrs.getValue("Plugin-Name");
                return new PluginInfo(pluginName, pluginVersion);
            }
        } catch (IOException e) {
            LOGGER.warn("Exception detected: {}", e.getMessage());
            LOGGER.debug("Exception stack trace", e);
            return null;
        }

        return null;
    }
}
