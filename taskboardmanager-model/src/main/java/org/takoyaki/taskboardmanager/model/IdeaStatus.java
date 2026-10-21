package org.takoyaki.taskboardmanager.model;

/** アイデアの検討状態です。 */
public enum IdeaStatus {
    CONSIDERING("検討中"), ADOPTED("採用"), REJECTED("見送り");

    private final String displayName;

    IdeaStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
