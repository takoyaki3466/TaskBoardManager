package org.takoyaki.taskboardmanager.model;

/** タスクの作業状態です。 */
public enum TaskStatus {
    TODO("未着手"), IN_PROGRESS("進行中"), BLOCKED("停止中"), DONE("完了");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
