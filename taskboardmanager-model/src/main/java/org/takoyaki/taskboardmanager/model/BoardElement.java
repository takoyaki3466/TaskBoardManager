package org.takoyaki.taskboardmanager.model;

import java.util.Objects;
import java.util.UUID;

/**
 * ボードに配置される要素の共通状態を保持します。
 */
public sealed abstract class BoardElement permits NodeElement, BoxElement {

    private final UUID id;
    private final String title;
    private final Position position;
    private final ElementSize size;

    protected BoardElement(UUID id, String title, Position position, ElementSize size) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = validateTitle(title);
        this.position = Objects.requireNonNull(position, "position");
        this.size = Objects.requireNonNull(size, "size");
    }

    public final UUID id() {
        return id;
    }

    public final String title() {
        return title;
    }

    public final Position position() {
        return position;
    }

    public final ElementSize size() {
        return size;
    }

    /** タイトルを変更した新しい要素を返します。 */
    public abstract BoardElement withTitle(String newTitle);

    /** 位置を変更した新しい要素を返します。 */
    public abstract BoardElement withPosition(Position newPosition);

    /** サイズを変更した新しい要素を返します。 */
    public abstract BoardElement withSize(ElementSize newSize);

    private static String validateTitle(String title) {
        String normalizedTitle = Objects.requireNonNull(title, "title").trim();
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("タイトルは空にできません。");
        }
        return normalizedTitle;
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        BoardElement other = (BoardElement) object;
        return id.equals(other.id)
                && title.equals(other.title)
                && position.equals(other.position)
                && size.equals(other.size)
                && Objects.equals(equalityState(), other.equalityState());
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), id, title, position, size, equalityState());
    }

    /** サブクラス固有状態を構造比較へ含めます。 */
    protected Object equalityState() {
        return null;
    }
}
