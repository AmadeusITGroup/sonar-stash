package org.sonar.plugins.stash;

import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.component.Component;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Resource;

public class DummyResourcePerspective implements ResourcePerspectives {
    @Override
    public <P extends Perspective> P as(Class<P> perspectiveClass, Resource resource) {
        return null;
    }

    @Override
    public <P extends Perspective> P as(Class<P> perspectiveClass, InputPath inputPath) {
        return null;
    }

    @Override
    public <P extends Perspective> P as(Class<P> perspectiveClass, Component component) {
        return null;
    }
}
