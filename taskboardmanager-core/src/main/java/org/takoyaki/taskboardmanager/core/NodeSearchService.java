package org.takoyaki.taskboardmanager.core;

import java.util.List;
import java.util.Locale;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;

/** 正規データを変更せず、文字列と属性条件でノードを検索します。 */
public final class NodeSearchService {

    public List<NodeElement> search(BoardDocument document, NodeSearchCriteria criteria) {
        String query = criteria.query().toLowerCase(Locale.ROOT);
        return document.elements().stream()
                .filter(NodeElement.class::isInstance)
                .map(NodeElement.class::cast)
                .filter(node -> matchesText(node, query))
                .filter(node -> criteria.nodeType() == null
                        || node.nodeType() == criteria.nodeType())
                .filter(node -> criteria.taskStatus() == null
                        || node.nodeType() == NodeType.TASK
                        && node.taskProperties().status() == criteria.taskStatus())
                .filter(node -> criteria.priority() == null
                        || node.nodeType() == NodeType.TASK
                        && node.taskProperties().priority() == criteria.priority())
                .filter(node -> criteria.questionStatus() == null
                        || node.nodeType() == NodeType.QUESTION
                        && node.questionProperties().status() == criteria.questionStatus())
                .toList();
    }

    private boolean matchesText(NodeElement node, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String searchable = (node.title() + " " + node.description() + " "
                + node.nodeType().name() + " " + node.nodeType().displayName() + " "
                + detailText(node)).toLowerCase(Locale.ROOT);
        return searchable.contains(query);
    }

    private String detailText(NodeElement node) {
        if (node.taskProperties() != null) {
            return node.taskProperties().status().name() + " "
                    + node.taskProperties().status() + " "
                    + node.taskProperties().priority().name() + " "
                    + node.taskProperties().priority();
        }
        if (node.questionProperties() != null) {
            return node.questionProperties().status().name() + " "
                    + node.questionProperties().status() + " "
                    + node.questionProperties().resolution();
        }
        if (node.decisionProperties() != null) {
            return node.decisionProperties().decision() + " "
                    + node.decisionProperties().rationale();
        }
        return "";
    }
}
