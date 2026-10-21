package org.takoyaki.taskboardmanager.desktop.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.RelationType;

/** PARENT_CHILDをアウトライン表示します。複数親のノードは各親の配下に現れます。 */
public final class OutlineView extends BorderPane {

    private final TreeView<OutlineEntry> tree = new TreeView<>();
    private BoardDocument document;
    private Consumer<UUID> selectionListener = ignored -> { };
    private UUID selectedId;

    public OutlineView() {
        getStyleClass().add("outline-view");
        tree.setShowRoot(false);
        tree.setCellFactory(ignored -> new TreeCell<>() {
            @Override
            protected void updateItem(OutlineEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldItem, newItem) -> {
                    if (newItem != null && newItem.getValue().id() != null) {
                        selectedId = newItem.getValue().id();
                        selectionListener.accept(newItem.getValue().id());
                    }
                });
        setCenter(tree);
    }

    public void setDocument(BoardDocument document) {
        this.document = Objects.requireNonNull(document, "document");
        refresh();
    }

    public void setSelectionListener(Consumer<UUID> listener) {
        selectionListener = Objects.requireNonNull(listener, "listener");
    }

    public void refresh() {
        TreeItem<OutlineEntry> root = new TreeItem<>(new OutlineEntry(null, "root"));
        root.setExpanded(true);
        tree.setRoot(root);
        if (document == null) {
            return;
        }
        Map<UUID, NodeElement> nodes = new HashMap<>();
        Map<UUID, List<UUID>> children = new HashMap<>();
        Set<UUID> childIds = new HashSet<>();
        document.elements().stream().filter(NodeElement.class::isInstance)
                .map(NodeElement.class::cast).forEach(node -> {
                    nodes.put(node.id(), node);
                    children.put(node.id(), new ArrayList<>());
                });
        document.connections().stream()
                .filter(connection -> connection.relationType() == RelationType.PARENT_CHILD)
                .filter(connection -> nodes.containsKey(connection.sourceElementId())
                        && nodes.containsKey(connection.targetElementId()))
                .forEach(connection -> {
                    children.get(connection.sourceElementId()).add(connection.targetElementId());
                    childIds.add(connection.targetElementId());
                });
        nodes.values().stream().filter(node -> !childIds.contains(node.id()))
                .sorted(java.util.Comparator.comparing(NodeElement::title))
                .map(node -> buildItem(node.id(), nodes, children, Set.of()))
                .forEach(root.getChildren()::add);
        if (selectedId != null) {
            selectNode(selectedId);
        }
    }

    public void selectNode(UUID elementId) {
        selectedId = elementId;
        TreeItem<OutlineEntry> item = find(tree.getRoot(), elementId);
        if (item != null) {
            expandParents(item);
            tree.getSelectionModel().select(item);
            tree.scrollTo(tree.getRow(item));
        }
    }

    private TreeItem<OutlineEntry> buildItem(
            UUID id, Map<UUID, NodeElement> nodes,
            Map<UUID, List<UUID>> children, Set<UUID> ancestors) {
        NodeElement node = nodes.get(id);
        TreeItem<OutlineEntry> item = new TreeItem<>(new OutlineEntry(
                id, node.nodeType().icon() + " " + node.title()));
        if (ancestors.contains(id)) {
            return item;
        }
        Set<UUID> nextAncestors = new HashSet<>(ancestors);
        nextAncestors.add(id);
        children.getOrDefault(id, List.of()).stream()
                .map(child -> buildItem(child, nodes, children, nextAncestors))
                .forEach(item.getChildren()::add);
        item.setExpanded(true);
        return item;
    }

    private TreeItem<OutlineEntry> find(TreeItem<OutlineEntry> item, UUID id) {
        if (item == null || id.equals(item.getValue().id())) {
            return item;
        }
        for (TreeItem<OutlineEntry> child : item.getChildren()) {
            TreeItem<OutlineEntry> found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void expandParents(TreeItem<OutlineEntry> item) {
        for (TreeItem<OutlineEntry> current = item.getParent();
                current != null;
                current = current.getParent()) {
            current.setExpanded(true);
        }
    }

    private record OutlineEntry(UUID id, String label) {
    }
}
