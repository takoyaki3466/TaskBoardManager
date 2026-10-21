package org.takoyaki.taskboardmanager.desktop.view;

import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import org.takoyaki.taskboardmanager.model.Connection;

/** Connectionモデルを関係種別に応じた曲線と方向マーカーで描画します。 */
public final class ConnectionView extends Group {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final double ARROW_LENGTH = 11.0;
    private static final double ARROW_WIDTH = 5.0;

    private final Connection connection;
    private final Path curve = new Path();
    private final Polygon arrow = new Polygon();
    private final MoveTo moveTo = new MoveTo();
    private final CubicCurveTo curveTo = new CubicCurveTo();

    public ConnectionView(Connection connection) {
        this.connection = connection;
        getStyleClass().add("connection-view");
        curve.getStyleClass().addAll(
                "connection-line", RelationStyle.cssClass(connection.relationType()));
        curve.getElements().addAll(moveTo, curveTo);
        arrow.getStyleClass().addAll(
                "connection-arrow", RelationStyle.cssClass(connection.relationType()));
        arrow.setVisible(RelationStyle.showsArrow(connection.relationType()));
        getChildren().addAll(curve, arrow);
    }

    public Connection connection() {
        return connection;
    }

    public void update(double startX, double startY, double endX, double endY) {
        double controlOffset = Math.max(60.0, Math.abs(endX - startX) * 0.45);
        moveTo.setX(startX);
        moveTo.setY(startY);
        curveTo.setControlX1(startX + controlOffset);
        curveTo.setControlY1(startY);
        curveTo.setControlX2(endX - controlOffset);
        curveTo.setControlY2(endY);
        curveTo.setX(endX);
        curveTo.setY(endY);
        updateArrow(curveTo.getControlX2(), curveTo.getControlY2(), endX, endY);
    }

    public void setSelected(boolean selected) {
        curve.pseudoClassStateChanged(SELECTED, selected);
        arrow.pseudoClassStateChanged(SELECTED, selected);
    }

    private void updateArrow(double fromX, double fromY, double endX, double endY) {
        double angle = Math.atan2(endY - fromY, endX - fromX);
        double baseX = endX - Math.cos(angle) * ARROW_LENGTH;
        double baseY = endY - Math.sin(angle) * ARROW_LENGTH;
        double perpendicularX = -Math.sin(angle) * ARROW_WIDTH;
        double perpendicularY = Math.cos(angle) * ARROW_WIDTH;
        arrow.getPoints().setAll(
                endX, endY,
                baseX + perpendicularX, baseY + perpendicularY,
                baseX - perpendicularX, baseY - perpendicularY);
    }
}
