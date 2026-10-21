package org.takoyaki.taskboardmanager.desktop.view;

import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.NodeElement;

/** モデルの具象型に対応する表示コンポーネントを生成します。 */
public final class ElementViewFactory {

    private ElementViewFactory() {
    }

    public static BoardElementView create(BoardElement element) {
        return switch (element) {
            case NodeElement node -> new NodeElementView(node);
            case BoxElement box -> new BoxElementView(box);
        };
    }
}
