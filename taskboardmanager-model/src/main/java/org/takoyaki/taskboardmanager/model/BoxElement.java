package org.takoyaki.taskboardmanager.model;

import java.util.UUID;

/** 自由に配置、リサイズできる矩形要素を表します。 */
public final class BoxElement extends BoardElement {

    public BoxElement(UUID id, String title, Position position, ElementSize size) {
        super(id, title, position, size);
    }

    @Override
    public BoxElement withTitle(String newTitle) {
        return new BoxElement(id(), newTitle, position(), size());
    }

    @Override
    public BoxElement withPosition(Position newPosition) {
        return new BoxElement(id(), title(), newPosition, size());
    }

    @Override
    public BoxElement withSize(ElementSize newSize) {
        return new BoxElement(id(), title(), position(), newSize);
    }
}
