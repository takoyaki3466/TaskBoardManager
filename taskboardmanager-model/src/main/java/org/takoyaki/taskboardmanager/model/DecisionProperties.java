package org.takoyaki.taskboardmanager.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** DECISIONノード固有の決定内容を保持します。 */
public record DecisionProperties(String decision, String rationale, LocalDateTime decidedAt) {

    public static final DecisionProperties DEFAULT = new DecisionProperties("", "", null);

    public DecisionProperties {
        decision = Objects.requireNonNull(decision, "decision").trim();
        rationale = Objects.requireNonNull(rationale, "rationale").trim();
    }
}
