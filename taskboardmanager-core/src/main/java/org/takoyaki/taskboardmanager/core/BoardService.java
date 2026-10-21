package org.takoyaki.taskboardmanager.core;

import java.util.Objects;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.DecisionProperties;
import org.takoyaki.taskboardmanager.model.ElementSize;
import org.takoyaki.taskboardmanager.model.IdeaProperties;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.Position;
import org.takoyaki.taskboardmanager.model.QuestionProperties;
import org.takoyaki.taskboardmanager.model.TaskProperties;

/** ボード要素の生成と編集操作を提供します。 */
public final class BoardService {

    private final BoardDocument document;

    public BoardService(BoardDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    public NodeElement createNode(String title, Position position) {
        NodeElement node = new NodeElement(
                UUID.randomUUID(), title, position, BoardConfiguration.DEFAULT_NODE_SIZE);
        document.addElement(node);
        return node;
    }

    public BoxElement createBox(String title, Position position) {
        BoxElement box = new BoxElement(
                UUID.randomUUID(), title, position, BoardConfiguration.DEFAULT_BOX_SIZE);
        document.addElement(box);
        return box;
    }

    public BoardElement moveElement(UUID elementId, Position position) {
        BoardElement element = requireElement(elementId).withPosition(position);
        document.updateElement(element);
        return element;
    }

    public BoardElement resizeElement(UUID elementId, ElementSize size) {
        BoardElement current = requireElement(elementId);
        if (!(current instanceof BoxElement)) {
            throw new IllegalArgumentException("リサイズできるのはボックスのみです。");
        }
        if (size.width() < BoardConfiguration.MINIMUM_BOX_WIDTH
                || size.height() < BoardConfiguration.MINIMUM_BOX_HEIGHT) {
            throw new IllegalArgumentException("ボックスが最小サイズを下回っています。");
        }
        BoardElement element = current.withSize(size);
        document.updateElement(element);
        return element;
    }

    public BoardElement renameElement(UUID elementId, String title) {
        BoardElement element = requireElement(elementId).withTitle(title);
        document.updateElement(element);
        return element;
    }

    /** ノードの意味と種類別属性をUUID・位置・接続を維持したまま更新します。 */
    public NodeElement updateNodeDetails(
            UUID elementId,
            NodeType nodeType,
            String description,
            TaskProperties taskProperties,
            QuestionProperties questionProperties,
            IdeaProperties ideaProperties,
            DecisionProperties decisionProperties) {
        BoardElement current = requireElement(elementId);
        if (!(current instanceof NodeElement node)) {
            throw new IllegalArgumentException("種類別属性を設定できるのはノードのみです。");
        }
        NodeElement updated = node.withDetails(
                nodeType, description, taskProperties,
                questionProperties, ideaProperties, decisionProperties);
        document.updateElement(updated);
        return updated;
    }

    public boolean deleteElement(UUID elementId) {
        return document.removeElement(elementId);
    }

    private BoardElement requireElement(UUID elementId) {
        return document.findElement(elementId)
                .orElseThrow(() -> new IllegalArgumentException("対象の要素が存在しません。"));
    }
}
