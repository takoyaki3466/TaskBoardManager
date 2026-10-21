package org.takoyaki.taskboardmanager.model;

/** タスク進捗の決定方法です。 */
public enum ProgressMode {
    MANUAL("手動"), CHILD_AVERAGE("子タスク平均");

    private final String displayName;

    ProgressMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
