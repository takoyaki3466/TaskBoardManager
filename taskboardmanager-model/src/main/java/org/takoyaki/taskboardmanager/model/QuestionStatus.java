package org.takoyaki.taskboardmanager.model;

/** 疑問の解決状態です。 */
public enum QuestionStatus {
    OPEN("未解決"), INVESTIGATING("調査中"), RESOLVED("解決済み");

    private final String displayName;

    QuestionStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
