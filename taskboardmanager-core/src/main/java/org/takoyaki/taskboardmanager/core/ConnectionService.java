package org.takoyaki.taskboardmanager.core;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.RelationType;

/** 要素間の接続作成、削除、検索を担当します。 */
public final class ConnectionService {

    private final BoardDocument document;

    public ConnectionService(BoardDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    public Connection createConnection(UUID sourceElementId, UUID targetElementId) {
        return createConnection(sourceElementId, targetElementId, RelationType.UNSPECIFIED);
    }

    public Connection createConnection(
            UUID sourceElementId, UUID targetElementId, RelationType relationType) {
        validateRelation(sourceElementId, targetElementId, relationType, null);
        Connection connection = new Connection(
                UUID.randomUUID(), sourceElementId, targetElementId, relationType);
        document.addConnection(connection);
        return connection;
    }

    /** 接続IDと端点を維持したまま関係種別を変更します。 */
    public Connection updateRelation(UUID connectionId, RelationType relationType) {
        Connection current = document.findConnection(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("対象の接続が存在しません。"));
        validateRelation(
                current.sourceElementId(), current.targetElementId(), relationType, connectionId);
        Connection updated = current.withRelationType(relationType);
        document.updateConnection(updated);
        return updated;
    }

    public boolean deleteConnection(UUID connectionId) {
        return document.removeConnection(connectionId);
    }

    public boolean canConnect(UUID sourceElementId, UUID targetElementId) {
        if (sourceElementId == null || targetElementId == null
                || sourceElementId.equals(targetElementId)) {
            return false;
        }
        if (document.findElement(sourceElementId).isEmpty()
                || document.findElement(targetElementId).isEmpty()) {
            return false;
        }
        return document.connections().stream().noneMatch(connection ->
                connection.sourceElementId().equals(sourceElementId)
                        && connection.targetElementId().equals(targetElementId));
    }

    public List<Connection> findRelatedConnections(UUID elementId) {
        return document.connections().stream()
                .filter(connection -> connection.sourceElementId().equals(elementId)
                        || connection.targetElementId().equals(elementId))
                .toList();
    }

    private void validateRelation(
            UUID sourceId, UUID targetId, RelationType relationType, UUID excludedConnectionId) {
        Objects.requireNonNull(relationType, "relationType");
        if (!canConnectIgnoring(sourceId, targetId, excludedConnectionId)) {
            throw new IllegalArgumentException("自己接続、重複接続、存在しない接続先は指定できません。");
        }
        if ((relationType == RelationType.PARENT_CHILD
                || relationType == RelationType.DEPENDS_ON)
                && hasPath(targetId, sourceId, relationType, excludedConnectionId, new HashSet<>())) {
            throw new IllegalArgumentException(
                    relationType == RelationType.PARENT_CHILD
                            ? "親子関係が循環するため設定できません。"
                            : "依存関係が循環するため設定できません。");
        }
    }

    private boolean canConnectIgnoring(UUID sourceId, UUID targetId, UUID excludedConnectionId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)
                || document.findElement(sourceId).isEmpty()
                || document.findElement(targetId).isEmpty()) {
            return false;
        }
        return document.connections().stream().noneMatch(connection ->
                !connection.id().equals(excludedConnectionId)
                        && connection.sourceElementId().equals(sourceId)
                        && connection.targetElementId().equals(targetId));
    }

    private boolean hasPath(
            UUID current,
            UUID goal,
            RelationType relationType,
            UUID excludedConnectionId,
            Set<UUID> visited) {
        if (current.equals(goal)) {
            return true;
        }
        if (!visited.add(current)) {
            return false;
        }
        for (Connection connection : document.connections()) {
            if (connection.id().equals(excludedConnectionId)
                    || connection.relationType() != relationType
                    || !connection.sourceElementId().equals(current)) {
                continue;
            }
            if (hasPath(
                    connection.targetElementId(), goal, relationType,
                    excludedConnectionId, visited)) {
                return true;
            }
        }
        return false;
    }
}
