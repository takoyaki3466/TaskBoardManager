package org.takoyaki.taskboardmanager.model;

/** タスクの重要度です。 */
public enum Priority {
    LOW("低"), NORMAL("通常"), HIGH("高"), CRITICAL("最優先");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
