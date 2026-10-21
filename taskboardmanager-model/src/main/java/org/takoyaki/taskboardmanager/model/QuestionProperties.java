package org.takoyaki.taskboardmanager.model;

import java.util.Objects;

/** QUESTIONノード固有の解決情報です。 */
public record QuestionProperties(QuestionStatus status, String resolution) {

    public static final QuestionProperties DEFAULT =
            new QuestionProperties(QuestionStatus.OPEN, "");

    public QuestionProperties {
        Objects.requireNonNull(status, "status");
        resolution = Objects.requireNonNull(resolution, "resolution").trim();
    }
}
