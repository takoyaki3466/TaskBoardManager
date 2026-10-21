package org.takoyaki.taskboardmanager.desktop.view;

import org.takoyaki.taskboardmanager.model.RelationType;

/** 論理的な接続関係をJavaFX CSSクラスと方向表示へ対応付けます。 */
final class RelationStyle {

    private RelationStyle() {
    }

    static String cssClass(RelationType relationType) {
        return "relation-" + relationType.name().toLowerCase().replace('_', '-');
    }

    static boolean showsArrow(RelationType relationType) {
        return relationType != RelationType.RELATED
                && relationType != RelationType.UNSPECIFIED;
    }
}
