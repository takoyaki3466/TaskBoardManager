package org.takoyaki.taskboardmanager.model;

import java.util.Objects;

/** IDEAノード固有の検討状態です。 */
public record IdeaProperties(IdeaStatus status) {

    public static final IdeaProperties DEFAULT = new IdeaProperties(IdeaStatus.CONSIDERING);

    public IdeaProperties {
        Objects.requireNonNull(status, "status");
    }
}
