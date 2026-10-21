package org.takoyaki.taskboardmanager.model;

/** 接続が表す論理関係です。 */
public enum RelationType {
    UNSPECIFIED("未指定"),
    PARENT_CHILD("親 → 子"),
    DEPENDS_ON("依存側 → 依存先"),
    RELATED("関連"),
    REFERENCES("参照"),
    BLOCKS("妨げる側 → 対象");

    private final String displayName;

    RelationType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
