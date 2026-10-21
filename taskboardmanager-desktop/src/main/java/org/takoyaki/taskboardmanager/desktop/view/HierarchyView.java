package org.takoyaki.taskboardmanager.desktop.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.CubicCurve;
import org.takoyaki.taskboardmanager.core.BoardConfiguration;
import org.takoyaki.taskboardmanager.core.HierarchicalLayoutEngine;
import org.takoyaki.taskboardmanager.core.LayoutRequest;
import org.takoyaki.taskboardmanager.core.LayoutResult;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.Position;
import org.takoyaki.taskboardmanager.model.RelationType;

/** PARENT_CHILDを左から右へ一時配置する、モデル座標を変更しない表示です。 */
public final class HierarchyView extends ScrollPane {

    private static final double MARGIN = 48.0;
    private final Pane canvas = new Pane();
    private final Map<UUID, VBox> cards = new LinkedHashMap<>();
    private BoardDocument document;
    private Consumer<UUID> selectionListener = ignored -> { };
    private UUID selectedId;

    public HierarchyView() {
        getStyleClass().add("hierarchy-view");
        canvas.getStyleClass().add("hierarchy-canvas");
        setContent(canvas);
        setPannable(true);
        setFitToHeight(false);
        setFitToWidth(false);
    }

    public void setDocument(BoardDocument document) {
        this.document = Objects.requireNonNull(document, "document");
        refresh();
    }

    public void setSelectionListener(Consumer<UUID> listener) {
        selectionListener = Objects.requireNonNull(listener, "listener");
    }

    public void refresh() {
        canvas.getChildren().clear();
        cards.clear();
        if (document == null) {
            return;
        }
        List<NodeElement> nodes = document.elements().stream()
                .filter(NodeElement.class::isInstance)
                .map(NodeElement.class::cast)
                .toList();
        LayoutResult layout = new HierarchicalLayoutEngine().layout(new LayoutRequest(
                nodes,
                document.connections().stream().toList(),
                BoardConfiguration.HIERARCHY_HORIZONTAL_GAP,
                BoardConfiguration.HIERARCHY_VERTICAL_GAP));
        Map<UUID, NodeElement> byId = nodes.stream().collect(java.util.stream.Collectors.toMap(
                NodeElement::id, node -> node));

        for (Connection connection : document.connections()) {
            NodeElement source = byId.get(connection.sourceElementId());
            NodeElement target = byId.get(connection.targetElementId());
            Position sourcePosition = layout.positions().get(connection.sourceElementId());
            Position targetPosition = layout.positions().get(connection.targetElementId());
            if (source == null || target == null || sourcePosition == null || targetPosition == null) {
                continue;
            }
            double startX = MARGIN + sourcePosition.x() + source.size().width();
            double startY = MARGIN + sourcePosition.y() + source.size().height() / 2.0;
            double endX = MARGIN + targetPosition.x();
            double endY = MARGIN + targetPosition.y() + target.size().height() / 2.0;
            CubicCurve curve = new CubicCurve(
                    startX, startY, (startX + endX) / 2.0, startY,
                    (startX + endX) / 2.0, endY, endX, endY);
            curve.getStyleClass().add("hierarchy-connection");
            if (connection.relationType() != RelationType.PARENT_CHILD) {
                curve.getStyleClass().add("hierarchy-secondary-connection");
            }
            curve.getStyleClass().add(RelationStyle.cssClass(connection.relationType()));
            canvas.getChildren().add(curve);
        }
        for (NodeElement node : nodes) {
            Position position = layout.positions().get(node.id());
            VBox card = createCard(node);
            card.relocate(MARGIN + position.x(), MARGIN + position.y());
            card.setPrefSize(node.size().width(), node.size().height());
            cards.put(node.id(), card);
            canvas.getChildren().add(card);
        }
        canvas.setPrefSize(
                Math.max(700.0, layout.bounds().width() + MARGIN * 2.0),
                Math.max(480.0, layout.bounds().height() + MARGIN * 2.0));
        applySelection();
    }

    public void selectNode(UUID elementId) {
        selectedId = elementId;
        applySelection();
        VBox card = cards.get(elementId);
        if (card != null && canvas.getWidth() > getViewportBounds().getWidth()) {
            setHvalue(Math.clamp(
                    card.getLayoutX() / Math.max(
                            1.0, canvas.getWidth() - getViewportBounds().getWidth()),
                    0.0, 1.0));
        }
        if (card != null && canvas.getHeight() > getViewportBounds().getHeight()) {
            setVvalue(Math.clamp(
                    card.getLayoutY() / Math.max(
                            1.0, canvas.getHeight() - getViewportBounds().getHeight()),
                    0.0, 1.0));
        }
    }

    private VBox createCard(NodeElement node) {
        Label type = new Label(node.nodeType().icon() + " " + node.nodeType().displayName());
        type.getStyleClass().add("node-type-badge");
        Label title = new Label(node.title());
        title.setWrapText(true);
        title.getStyleClass().add("element-title");
        VBox card = new VBox(7.0, type, title);
        card.getStyleClass().addAll(
                "hierarchy-card", "node-type-" + node.nodeType().name().toLowerCase());
        card.setOnMouseClicked(event -> {
            selectedId = node.id();
            applySelection();
            selectionListener.accept(node.id());
            event.consume();
        });
        return card;
    }

    private void applySelection() {
        cards.forEach((id, card) -> card.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"), id.equals(selectedId)));
    }
}
