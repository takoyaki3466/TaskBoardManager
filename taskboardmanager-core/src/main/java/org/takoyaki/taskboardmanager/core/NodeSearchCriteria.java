package org.takoyaki.taskboardmanager.core;

import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.Priority;
import org.takoyaki.taskboardmanager.model.QuestionStatus;
import org.takoyaki.taskboardmanager.model.TaskStatus;

/** ノード検索の文字列と任意フィルターです。 */
public record NodeSearchCriteria(
        String query,
        NodeType nodeType,
        TaskStatus taskStatus,
        Priority priority,
        QuestionStatus questionStatus) {

    public NodeSearchCriteria {
        query = query == null ? "" : query.trim();
    }
}
