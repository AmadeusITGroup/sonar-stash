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

    private static final String ERROR_DETAILS = "Exception detected: {}";
    private static final String ERROR_STACK   = "Exception stack trace";


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

                if (pluginClass == null || ! classesMatching(pluginClass, klass)) {
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

    private static boolean classesMatching(String className, Class right) {

        boolean doesMatch = false; 

        // Let's compare the plugin's class with another one (without '==', see SQUID:S1872)
        try {
            Class<?> left   = Class.forName(className);
            Object leftObj  = left.getClass().newInstance();
            Object rightObj = right.getClass().newInstance();
        
            if (  left.isInstance(rightObj)
             ||  right.isInstance(leftObj) ) {
                doesMatch = true;
            }

        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
            LOGGER.warn(ERROR_DETAILS, e.getMessage());
            LOGGER.debug(ERROR_STACK, e);
        }
        return doesMatch;
    }
}
