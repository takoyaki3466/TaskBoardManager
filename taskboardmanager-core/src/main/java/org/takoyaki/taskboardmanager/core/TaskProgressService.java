package org.takoyaki.taskboardmanager.core;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.ProgressMode;
import org.takoyaki.taskboardmanager.model.RelationType;
import org.takoyaki.taskboardmanager.model.TaskProperties;
import org.takoyaki.taskboardmanager.model.TaskStatus;

/** 親タスクの直接の子TASKから単純平均進捗を計算します。 */
public final class TaskProgressService {

    public int childAverage(BoardDocument document, UUID parentId) {
        Integer average = childAverageOrNull(document, parentId);
        if (average == null) {
            throw new IllegalArgumentException("集計対象の直接の子タスクがありません。");
        }
        return average;
    }

    private Integer childAverageOrNull(BoardDocument document, UUID parentId) {
        Set<UUID> childIds = new LinkedHashSet<>();
        for (Connection connection : document.connections()) {
            if (connection.relationType() == RelationType.PARENT_CHILD
                    && connection.sourceElementId().equals(parentId)) {
                childIds.add(connection.targetElementId());
            }
        }
        int total = 0;
        int count = 0;
        for (UUID childId : childIds) {
            if (document.findElement(childId).orElse(null) instanceof NodeElement node
                    && node.nodeType() == NodeType.TASK) {
                total += node.taskProperties().progress();
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        return Math.round((float) total / count);
    }

    /** CHILD_AVERAGEの親タスクを下位の変更に追従させます。子がない親は現在値を維持します。 */
    public void refreshChildAverages(BoardDocument document) {
        int maximumPasses = document.elements().size();
        for (int pass = 0; pass < maximumPasses; pass++) {
            boolean changed = false;
            for (NodeElement node : document.elements().stream()
                    .filter(NodeElement.class::isInstance)
                    .map(NodeElement.class::cast)
                    .toList()) {
                if (node.nodeType() != NodeType.TASK
                        || node.taskProperties().progressMode() != ProgressMode.CHILD_AVERAGE) {
                    continue;
                }
                Integer average = childAverageOrNull(document, node.id());
                if (average == null || average == node.taskProperties().progress()) {
                    continue;
                }
                TaskProperties current = node.taskProperties();
                TaskStatus status = average == 100
                        ? TaskStatus.DONE
                        : current.status() == TaskStatus.DONE
                                ? TaskStatus.IN_PROGRESS
                                : current.status();
                TaskProperties updatedProperties = new TaskProperties(
                        status,
                        average,
                        current.priority(),
                        current.progressMode(),
                        current.startDate(),
                        current.dueDate());
                document.updateElement(node.withDetails(
                        node.nodeType(),
                        node.description(),
                        updatedProperties,
                        null,
                        null,
                        null));
                changed = true;
            }
            if (!changed) {
                return;
            }
        }
    }
}
