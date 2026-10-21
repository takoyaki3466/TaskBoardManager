package org.takoyaki.taskboardmanager.desktop.view;

import java.util.UUID;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.takoyaki.taskboardmanager.model.BoardElement;

/** BoardElementを表示するJavaFXコンポーネントの基底クラスです。 */
public abstract class BoardElementView extends StackPane {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CONNECTION_TARGET =
            PseudoClass.getPseudoClass("connection-target");
    private final UUID elementId;
    private final Label titleLabel = new Label();
    private final Circle connectionHandle = new Circle(6.0);

    protected BoardElementView(BoardElement element, String styleClass) {
        elementId = element.id();
        getStyleClass().addAll("board-element", styleClass);
        titleLabel.getStyleClass().add("element-title");
        titleLabel.setWrapText(true);
        connectionHandle.getStyleClass().add("connection-handle");
        connectionHandle.setManaged(false);
        connectionHandle.setVisible(false);
        getChildren().addAll(titleLabel, connectionHandle);
        setAlignment(Pos.CENTER_LEFT);
        applyElement(element);

        hoverProperty().addListener((observable, oldValue, hovered) ->
                connectionHandle.setVisible(hovered || isSelected()));
    }

    public UUID elementId() {
        return elementId;
    }

    public Circle connectionHandle() {
        return connectionHandle;
    }

    public void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED, selected);
        connectionHandle.setVisible(selected || isHover());
    }

    public boolean isSelected() {
        return getPseudoClassStates().contains(SELECTED);
    }

    public void setConnectionTarget(boolean connectionTarget) {
        pseudoClassStateChanged(CONNECTION_TARGET, connectionTarget);
    }

    /** モデルの最新状態を表示へ反映します。 */
    public void update(BoardElement element) {
        applyElement(element);
    }

    private void applyElement(BoardElement element) {
        titleLabel.setText(element.title());
        relocate(element.position().x(), element.position().y());
        setPrefSize(element.size().width(), element.size().height());
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        connectionHandle.setCenterX(element.size().width());
        connectionHandle.setCenterY(element.size().height() / 2.0);
    }
}
