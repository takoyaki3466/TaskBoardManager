package org.takoyaki.taskboardmanager.desktop.view;

import java.util.List;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.Connection;

/** インスペクターへ通知する現在の選択内容です。 */
public record SelectionInfo(List<BoardElement> elements, Connection connection) {

    public SelectionInfo {
        elements = List.copyOf(elements);
        if (!elements.isEmpty() && connection != null) {
            throw new IllegalArgumentException("要素と接続は同時に選択できません。");
        }
    }

    public static SelectionInfo empty() {
        return new SelectionInfo(List.of(), null);
    }

    public boolean isEmpty() {
        return elements.isEmpty() && connection == null;
    }
}
