package org.takoyaki.taskboardmanager.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.takoyaki.taskboardmanager.model.DocumentChange.ChangeKind;

/** 要素と接続を一貫した状態で保持するボード集約です。 */
public final class BoardDocument {

    private final UUID documentId;
    private String title;
    private final Map<UUID, BoardElement> elements = new LinkedHashMap<>();
    private final Map<UUID, Connection> connections = new LinkedHashMap<>();
    private ViewSettings viewSettings = ViewSettings.DEFAULT;
    private final List<Consumer<DocumentChange>> changeListeners = new CopyOnWriteArrayList<>();
    private long revision;

    public BoardDocument(UUID documentId, String title) {
        this.documentId = Objects.requireNonNull(documentId, "documentId");
        this.title = normalizeTitle(title);
    }

    public UUID documentId() {
        return documentId;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        String normalizedTitle = normalizeTitle(title);
        if (!normalizedTitle.equals(this.title)) {
            this.title = normalizedTitle;
            publish(ChangeKind.DOCUMENT_RESET, documentId);
        }
    }

    public Collection<BoardElement> elements() {
        return Collections.unmodifiableCollection(elements.values());
    }

    public Collection<Connection> connections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    public ViewSettings viewSettings() {
        return viewSettings;
    }

    public void setViewSettings(ViewSettings viewSettings) {
        ViewSettings validated = Objects.requireNonNull(viewSettings, "viewSettings");
        if (!validated.equals(this.viewSettings)) {
            this.viewSettings = validated;
            publish(ChangeKind.VIEW_UPDATED, documentId);
        }
    }

    public long revision() {
        return revision;
    }

    public void addChangeListener(Consumer<DocumentChange> listener) {
        changeListeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeChangeListener(Consumer<DocumentChange> listener) {
        changeListeners.remove(listener);
    }

    /** 現在の完全な論理状態を不変スナップショットとして返します。 */
    public BoardSnapshot snapshot() {
        return new BoardSnapshot(
                documentId,
                title,
                elements.values().stream().toList(),
                connections.values().stream().toList(),
                viewSettings);
    }

    /**
     * 同じドキュメントの検証済みスナップショットを復元します。
     * 復元前に一時ドキュメントへ全データを組み立てるため、失敗時は現在状態を保持します。
     *
     * @param snapshot 復元する状態
     */
    public void restore(BoardSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!documentId.equals(snapshot.documentId())) {
            throw new IllegalArgumentException("異なるドキュメントの状態は復元できません。");
        }
        BoardDocument validated = new BoardDocument(snapshot.documentId(), snapshot.title());
        for (BoardElement element : snapshot.elements()) {
            validated.addElement(element);
        }
        for (Connection connection : snapshot.connections()) {
            validated.addConnection(connection);
        }
        validated.setViewSettings(snapshot.viewSettings());

        title = validated.title;
        elements.clear();
        elements.putAll(validated.elements);
        connections.clear();
        connections.putAll(validated.connections);
        viewSettings = validated.viewSettings;
        publish(ChangeKind.DOCUMENT_RESET, documentId);
    }

    public Optional<BoardElement> findElement(UUID elementId) {
        return Optional.ofNullable(elements.get(Objects.requireNonNull(elementId, "elementId")));
    }

    public Optional<Connection> findConnection(UUID connectionId) {
        return Optional.ofNullable(connections.get(Objects.requireNonNull(connectionId, "connectionId")));
    }

    /** 重複IDを検証して要素を追加します。 */
    public void addElement(BoardElement element) {
        Objects.requireNonNull(element, "element");
        if (elements.putIfAbsent(element.id(), element) != null) {
            throw new IllegalArgumentException("同じIDの要素が既に存在します。");
        }
        publish(ChangeKind.ELEMENT_ADDED, element.id());
    }

    /** 既存要素を同じIDの新しい状態へ置き換えます。 */
    public void updateElement(BoardElement element) {
        Objects.requireNonNull(element, "element");
        BoardElement existing = elements.get(element.id());
        if (existing == null) {
            throw new IllegalArgumentException("更新対象の要素が存在しません。");
        }
        if (existing.equals(element)) {
            return;
        }
        elements.put(element.id(), element);
        publish(ChangeKind.ELEMENT_UPDATED, element.id());
    }

    /** 要素と、その要素を参照する接続を削除します。 */
    public boolean removeElement(UUID elementId) {
        Objects.requireNonNull(elementId, "elementId");
        if (elements.remove(elementId) == null) {
            return false;
        }
        List<UUID> removedConnectionIds = connections.values().stream()
                .filter(connection -> connection.sourceElementId().equals(elementId)
                        || connection.targetElementId().equals(elementId))
                .map(Connection::id)
                .toList();
        for (UUID connectionId : removedConnectionIds) {
            connections.remove(connectionId);
            publish(ChangeKind.CONNECTION_REMOVED, connectionId);
        }
        publish(ChangeKind.ELEMENT_REMOVED, elementId);
        return true;
    }

    /** 参照先と重複を検証して接続を追加します。 */
    public void addConnection(Connection connection) {
        Objects.requireNonNull(connection, "connection");
        if (!elements.containsKey(connection.sourceElementId())
                || !elements.containsKey(connection.targetElementId())) {
            throw new IllegalArgumentException("接続先の要素が存在しません。");
        }
        if (connections.containsKey(connection.id())) {
            throw new IllegalArgumentException("同じIDの接続が既に存在します。");
        }
        boolean duplicated = connections.values().stream().anyMatch(existing ->
                existing.sourceElementId().equals(connection.sourceElementId())
                        && existing.targetElementId().equals(connection.targetElementId()));
        if (duplicated) {
            throw new IllegalArgumentException("同じ方向の接続が既に存在します。");
        }
        connections.put(connection.id(), connection);
        publish(ChangeKind.CONNECTION_ADDED, connection.id());
    }

    public void updateConnection(Connection connection) {
        Objects.requireNonNull(connection, "connection");
        Connection existing = connections.get(connection.id());
        if (existing == null) {
            throw new IllegalArgumentException("更新対象の接続が存在しません。");
        }
        if (!existing.sourceElementId().equals(connection.sourceElementId())
                || !existing.targetElementId().equals(connection.targetElementId())) {
            throw new IllegalArgumentException("接続更新で端点IDは変更できません。");
        }
        if (existing.equals(connection)) {
            return;
        }
        connections.put(connection.id(), connection);
        publish(ChangeKind.CONNECTION_UPDATED, connection.id());
    }

    public boolean removeConnection(UUID connectionId) {
        UUID validatedId = Objects.requireNonNull(connectionId, "connectionId");
        if (connections.remove(validatedId) == null) {
            return false;
        }
        publish(ChangeKind.CONNECTION_REMOVED, validatedId);
        return true;
    }

    private void publish(ChangeKind kind, UUID subjectId) {
        revision++;
        DocumentChange change = new DocumentChange(kind, subjectId, revision);
        for (Consumer<DocumentChange> listener : changeListeners) {
            listener.accept(change);
        }
    }

    private static String normalizeTitle(String title) {
        String normalizedTitle = Objects.requireNonNull(title, "title").trim();
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("ボード名は空にできません。");
        }
        return normalizedTitle;
    }
}
