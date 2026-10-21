package org.takoyaki.taskboardmanager.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.Position;
import org.takoyaki.taskboardmanager.model.RelationType;

/** PARENT_CHILDのDAGを左から右へ決定的に配置します。 */
public final class HierarchicalLayoutEngine {

    public LayoutResult layout(LayoutRequest request) {
        List<NodeElement> nodes = request.nodes().stream()
                .sorted(Comparator.comparing(node -> node.id().toString()))
                .toList();
        Map<UUID, NodeElement> nodeById = new LinkedHashMap<>();
        Map<UUID, List<UUID>> children = new HashMap<>();
        Map<UUID, Integer> indegree = new HashMap<>();
        Map<UUID, Integer> depth = new HashMap<>();
        for (NodeElement node : nodes) {
            nodeById.put(node.id(), node);
            children.put(node.id(), new ArrayList<>());
            indegree.put(node.id(), 0);
            depth.put(node.id(), 0);
        }
        for (Connection connection : request.connections()) {
            if (connection.relationType() != RelationType.PARENT_CHILD
                    || !nodeById.containsKey(connection.sourceElementId())
                    || !nodeById.containsKey(connection.targetElementId())) {
                continue;
            }
            children.get(connection.sourceElementId()).add(connection.targetElementId());
            indegree.compute(connection.targetElementId(), (id, value) -> value + 1);
        }
        children.values().forEach(list -> list.sort(Comparator.comparing(UUID::toString)));

        Queue<UUID> queue = new ArrayDeque<>();
        for (NodeElement node : nodes) {
            if (indegree.get(node.id()) == 0) {
                queue.add(node.id());
            }
        }
        while (!queue.isEmpty()) {
            UUID parent = queue.remove();
            for (UUID child : children.get(parent)) {
                depth.put(child, Math.max(depth.get(child), depth.get(parent) + 1));
                int remaining = indegree.compute(child, (id, value) -> value - 1);
                if (remaining == 0) {
                    queue.add(child);
                }
            }
        }

        Map<Integer, List<NodeElement>> levels = new LinkedHashMap<>();
        for (NodeElement node : nodes) {
            levels.computeIfAbsent(depth.get(node.id()), ignored -> new ArrayList<>()).add(node);
        }
        levels.values().forEach(level -> level.sort(
                Comparator.comparing(NodeElement::title).thenComparing(node -> node.id().toString())));

        Map<UUID, Position> positions = new LinkedHashMap<>();
        double x = 0.0;
        double maximumBottom = 0.0;
        for (Map.Entry<Integer, List<NodeElement>> entry : levels.entrySet()) {
            double y = 0.0;
            double maximumWidth = 0.0;
            for (NodeElement node : entry.getValue()) {
                positions.put(node.id(), new Position(x, y));
                maximumWidth = Math.max(maximumWidth, node.size().width());
                y += node.size().height() + request.verticalGap();
                maximumBottom = Math.max(maximumBottom, y - request.verticalGap());
            }
            x += maximumWidth + request.horizontalGap();
        }
        double width = levels.isEmpty() ? 0.0 : Math.max(0.0, x - request.horizontalGap());
        return new LayoutResult(
                positions, new LayoutResult.LayoutBounds(0.0, 0.0, width, maximumBottom));
    }
}
