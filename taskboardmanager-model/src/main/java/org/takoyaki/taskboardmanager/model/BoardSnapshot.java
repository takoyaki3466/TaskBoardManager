package org.takoyaki.taskboardmanager.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Undo/Redoと保存状態比較に利用する、JavaFX非依存のボードスナップショットです。
 *
 * @param documentId ドキュメントID
 * @param title ボードタイトル
 * @param elements 要素一覧
 * @param connections 接続一覧
 * @param viewSettings 表示設定
 */
public record BoardSnapshot(
        UUID documentId,
        String title,
        List<BoardElement> elements,
        List<Connection> connections,
        ViewSettings viewSettings) {

    public BoardSnapshot {
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(title, "title");
        elements = List.copyOf(elements);
        connections = List.copyOf(connections);
        Objects.requireNonNull(viewSettings, "viewSettings");
    }
}
