package org.takoyaki.taskboardmanager.model;

import java.util.Objects;
import java.util.UUID;

/** 種類と必要な専用属性を持つ情報ノードです。 */
public final class NodeElement extends BoardElement {

    private final NodeType nodeType;
    private final String description;
    private final TaskProperties taskProperties;
    private final QuestionProperties questionProperties;
    private final IdeaProperties ideaProperties;
    private final DecisionProperties decisionProperties;

    public NodeElement(UUID id, String title, Position position, ElementSize size) {
        this(id, title, position, size, NodeType.GENERAL, "", null, null, null, null);
    }

    public NodeElement(
            UUID id,
            String title,
            Position position,
            ElementSize size,
            NodeType nodeType,
            String description,
            TaskProperties taskProperties,
            QuestionProperties questionProperties,
            IdeaProperties ideaProperties,
            DecisionProperties decisionProperties) {
        super(id, title, position, size);
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType");
        this.description = Objects.requireNonNull(description, "description").trim();
        this.taskProperties = nodeType == NodeType.TASK
                ? Objects.requireNonNullElse(taskProperties, TaskProperties.DEFAULT) : null;
        this.questionProperties = nodeType == NodeType.QUESTION
                ? Objects.requireNonNullElse(questionProperties, QuestionProperties.DEFAULT) : null;
        this.ideaProperties = nodeType == NodeType.IDEA
                ? Objects.requireNonNullElse(ideaProperties, IdeaProperties.DEFAULT) : null;
        this.decisionProperties = nodeType == NodeType.DECISION
                ? Objects.requireNonNullElse(decisionProperties, DecisionProperties.DEFAULT) : null;
    }

    public NodeType nodeType() {
        return nodeType;
    }

    public String description() {
        return description;
    }

    public TaskProperties taskProperties() {
        return taskProperties;
    }

    public QuestionProperties questionProperties() {
        return questionProperties;
    }

    public IdeaProperties ideaProperties() {
        return ideaProperties;
    }

    public DecisionProperties decisionProperties() {
        return decisionProperties;
    }

    public NodeElement withDetails(
            NodeType newType,
            String newDescription,
            TaskProperties newTaskProperties,
            QuestionProperties newQuestionProperties,
            IdeaProperties newIdeaProperties,
            DecisionProperties newDecisionProperties) {
        return new NodeElement(
                id(), title(), position(), size(), newType, newDescription,
                newTaskProperties, newQuestionProperties,
                newIdeaProperties, newDecisionProperties);
    }

    @Override
    public NodeElement withTitle(String newTitle) {
        return copy(newTitle, position(), size());
    }

    @Override
    public NodeElement withPosition(Position newPosition) {
        return copy(title(), newPosition, size());
    }

    @Override
    public NodeElement withSize(ElementSize newSize) {
        return copy(title(), position(), newSize);
    }

    @Override
    protected Object equalityState() {
        return new NodeState(
                nodeType, description, taskProperties,
                questionProperties, ideaProperties, decisionProperties);
    }

    private NodeElement copy(String newTitle, Position newPosition, ElementSize newSize) {
        return new NodeElement(
                id(), newTitle, newPosition, newSize, nodeType, description,
                taskProperties, questionProperties, ideaProperties, decisionProperties);
    }

    private record NodeState(
            NodeType nodeType,
            String description,
            TaskProperties taskProperties,
            QuestionProperties questionProperties,
            IdeaProperties ideaProperties,
            DecisionProperties decisionProperties) {
    }
}
