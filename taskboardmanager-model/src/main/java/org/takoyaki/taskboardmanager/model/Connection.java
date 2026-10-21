package org.takoyaki.taskboardmanager.model;

import java.util.Objects;
import java.util.UUID;

/**
 * 2つのボード要素間の有向接続を表します。
 *
 * @param id 接続ID
 * @param sourceElementId 始点要素ID
 * @param targetElementId 終点要素ID
 */
public record Connection(
        UUID id,
        UUID sourceElementId,
        UUID targetElementId,
        RelationType relationType) {

    public Connection(UUID id, UUID sourceElementId, UUID targetElementId) {
        this(id, sourceElementId, targetElementId, RelationType.UNSPECIFIED);
    }

    public Connection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceElementId, "sourceElementId");
        Objects.requireNonNull(targetElementId, "targetElementId");
        Objects.requireNonNull(relationType, "relationType");
        if (sourceElementId.equals(targetElementId)) {
            throw new IllegalArgumentException("自己接続は作成できません。");
        }
    }

    public Connection withRelationType(RelationType newRelationType) {
        return new Connection(id, sourceElementId, targetElementId, newRelationType);
    }
}
