package org.takoyaki.taskboardmanager.desktop.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;

/** 情報ノードを描画します。 */
public final class NodeElementView extends BoardElementView {

    private final Label typeBadge = new Label();
    private final ProgressBar progressBar = new ProgressBar();
    private final Label taskSummary = new Label();

    public NodeElementView(NodeElement element) {
        super(element, "node-element");
        typeBadge.getStyleClass().add("node-type-badge");
        progressBar.getStyleClass().add("node-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        taskSummary.getStyleClass().add("task-summary");
        getChildren().addAll(typeBadge, progressBar, taskSummary);
        StackPane.setAlignment(typeBadge, Pos.TOP_RIGHT);
        StackPane.setAlignment(progressBar, Pos.BOTTOM_CENTER);
        StackPane.setAlignment(taskSummary, Pos.BOTTOM_RIGHT);
        update(element);
    }

    @Override
    public void update(BoardElement element) {
        super.update(element);
        if (!(element instanceof NodeElement node) || typeBadge == null) {
            return;
        }
        getStyleClass().removeIf(style -> style.startsWith("node-type-"));
        getStyleClass().add("node-type-" + node.nodeType().name().toLowerCase());
        typeBadge.setText(node.nodeType().icon() + " " + node.nodeType().displayName());
        boolean task = node.nodeType() == NodeType.TASK;
        progressBar.setVisible(task);
        progressBar.setManaged(task);
        taskSummary.setVisible(task);
        taskSummary.setManaged(task);
        if (task) {
            progressBar.setProgress(node.taskProperties().progress() / 100.0);
            taskSummary.setText(node.taskProperties().progress() + "% / "
                    + node.taskProperties().status() + " / "
                    + node.taskProperties().priority());
        }
    }
}
