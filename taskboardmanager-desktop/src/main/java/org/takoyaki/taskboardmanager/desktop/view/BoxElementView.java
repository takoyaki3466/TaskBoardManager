package org.takoyaki.taskboardmanager.desktop.view;

import javafx.scene.shape.Rectangle;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;

/** 矩形ボックスを描画し、リサイズハンドルを公開します。 */
public final class BoxElementView extends BoardElementView {

    private final Rectangle resizeHandle = new Rectangle(12.0, 12.0);

    public BoxElementView(BoxElement element) {
        super(element, "box-element");
        resizeHandle.getStyleClass().add("resize-handle");
        resizeHandle.setManaged(false);
        resizeHandle.setVisible(false);
        getChildren().add(resizeHandle);
        update(element);
        hoverProperty().addListener((observable, oldValue, hovered) ->
                resizeHandle.setVisible(hovered || isSelected()));
    }

    public Rectangle resizeHandle() {
        return resizeHandle;
    }

    @Override
    public void update(BoardElement element) {
        super.update(element);
        if (resizeHandle != null) {
            resizeHandle.setLayoutX(element.size().width() - resizeHandle.getWidth());
            resizeHandle.setLayoutY(element.size().height() - resizeHandle.getHeight());
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        resizeHandle.setVisible(selected || isHover());
    }

}
