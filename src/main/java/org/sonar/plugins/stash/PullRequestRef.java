package org.sonar.plugins.stash;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PullRequestRef {
    public abstract String project();
    public abstract String repository();
    public abstract int pullRequestId();


    public static Builder builder() {
        return new AutoValue_PullRequestRef.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setProject(String value);
        public abstract Builder setRepository(String value);
        public abstract Builder setPullRequestId(int value);
        public abstract PullRequestRef build();
    }
}
