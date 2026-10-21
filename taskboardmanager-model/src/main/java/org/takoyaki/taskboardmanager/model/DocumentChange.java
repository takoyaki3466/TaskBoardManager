package org.takoyaki.taskboardmanager.model;

import java.util.UUID;

/** 将来の同期処理でも利用できる、最小限のドキュメント変更通知です。 */
public record DocumentChange(ChangeKind kind, UUID subjectId, long revision) {

    /** 変更された正規データの分類です。 */
    public enum ChangeKind {
        ELEMENT_ADDED,
        ELEMENT_REMOVED,
        ELEMENT_UPDATED,
        CONNECTION_ADDED,
        CONNECTION_REMOVED,
        CONNECTION_UPDATED,
        DOCUMENT_RESET,
        VIEW_UPDATED
    }
}
