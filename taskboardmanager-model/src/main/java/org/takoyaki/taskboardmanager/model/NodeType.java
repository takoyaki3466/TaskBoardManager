package org.takoyaki.taskboardmanager.model;

/** ノードが表す情報の意味です。 */
public enum NodeType {
    GENERAL("●", "通常"),
    GOAL("◎", "目標"),
    TASK("✓", "タスク"),
    QUESTION("?", "疑問"),
    IDEA("💡", "アイデア"),
    NOTE("≡", "メモ"),
    DECISION("◆", "決定事項");

    private final String icon;
    private final String displayName;

    NodeType(String icon, String displayName) {
        this.icon = icon;
        this.displayName = displayName;
    }

    public String icon() {
        return icon;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return icon + " " + displayName;
    }
}
