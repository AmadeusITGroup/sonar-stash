package org.sonar.plugins.stash;

import org.sonar.plugins.stash.client.StashClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class PluginUtils {
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
                if (attrs == null)
                    continue;
                String pluginClass = attrs.getValue("Plugin-Class");
                if (pluginClass == null)
                    continue;
                if (!pluginClass.equals(klass.getName()))
                    continue;

                String pluginVersion = attrs.getValue("Plugin-Version");
                String pluginName = attrs.getValue("Plugin-Name");
                return new PluginInfo(pluginName, pluginVersion);
            }
        } catch (IOException e) {
            return null;
        }

        return null;
    }
}
