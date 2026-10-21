package org.takoyaki.taskboardmanager.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.DecisionProperties;
import org.takoyaki.taskboardmanager.model.ElementSize;
import org.takoyaki.taskboardmanager.model.IdeaProperties;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.Position;
import org.takoyaki.taskboardmanager.model.QuestionProperties;
import org.takoyaki.taskboardmanager.model.RelationType;
import org.takoyaki.taskboardmanager.model.TaskProperties;
import org.takoyaki.taskboardmanager.model.ViewSettings;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.ConnectionData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.DecisionData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.ElementData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.IdeaData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.QuestionData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.TaskData;

/** JSONデータを完全に検証してからドメインモデルへ変換します。 */
public final class DocumentValidator {

    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final int LEGACY_SCHEMA_VERSION = 1;

    /**
     * ファイルデータを検証し、新しいドキュメントとして構築します。
     *
     * @throws InvalidBoardFileException 必須値または整合性が不正な場合
     */
    public BoardDocument toDocument(BoardFileData data) throws InvalidBoardFileException {
        try {
            require(data != null, "ファイル内容が空です。");
            require(data.schemaVersion() == CURRENT_SCHEMA_VERSION
                            || data.schemaVersion() == LEGACY_SCHEMA_VERSION,
                    "対応していないスキーマバージョンです: " + data.schemaVersion());
            require(data.documentId() != null, "documentIdがありません。");
            require(data.title() != null, "titleがありません。");
            require(data.elements() != null, "elementsがありません。");
            require(data.connections() != null, "connectionsがありません。");
            require(data.viewSettings() != null, "viewSettingsがありません。");

            BoardDocument document = new BoardDocument(data.documentId(), data.title());
            Set<UUID> elementIds = new HashSet<>();
            for (ElementData elementData : data.elements()) {
                require(elementData != null, "nullの要素が含まれています。");
                require(elementData.id() != null, "要素IDがありません。");
                require(elementData.kind() != null, "要素種別がありません。");
                require(elementIds.add(elementData.id()), "要素IDが重複しています。");
                Position position = new Position(elementData.x(), elementData.y());
                ElementSize size = new ElementSize(elementData.width(), elementData.height());
                BoardElement element = switch (elementData.kind()) {
                    case NODE -> createNode(elementData, position, size, data.schemaVersion());
                    case BOX -> new BoxElement(
                            elementData.id(), elementData.title(), position, size);
                };
                document.addElement(element);
            }

            Set<UUID> connectionIds = new HashSet<>();
            for (ConnectionData connectionData : data.connections()) {
                require(connectionData != null, "nullの接続が含まれています。");
                require(connectionData.id() != null, "接続IDがありません。");
                require(connectionIds.add(connectionData.id()), "接続IDが重複しています。");
                RelationType relationType = connectionData.relationType();
                if (relationType == null && data.schemaVersion() == LEGACY_SCHEMA_VERSION) {
                    relationType = RelationType.UNSPECIFIED;
                }
                require(relationType != null, "接続関係の種類がありません。");
                document.addConnection(new Connection(
                        connectionData.id(),
                        connectionData.sourceElementId(),
                        connectionData.targetElementId(),
                        relationType));
            }
            validateAcyclic(document, RelationType.PARENT_CHILD);
            validateAcyclic(document, RelationType.DEPENDS_ON);
            document.setViewSettings(new ViewSettings(
                    data.viewSettings().zoom(),
                    data.viewSettings().offsetX(),
                    data.viewSettings().offsetY()));
            return document;
        } catch (InvalidBoardFileException exception) {
            throw exception;
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidBoardFileException(
                    "ボードデータが不正です: " + exception.getMessage(), exception);
        }
    }

    private NodeElement createNode(
            ElementData data, Position position, ElementSize size, int schemaVersion)
            throws InvalidBoardFileException {
        NodeType nodeType = data.nodeType();
        if (nodeType == null && schemaVersion == LEGACY_SCHEMA_VERSION) {
            nodeType = NodeType.GENERAL;
        }
        require(nodeType != null, "ノード種類がありません。");
        String description = data.description() == null ? "" : data.description();
        return new NodeElement(
                data.id(), data.title(), position, size, nodeType, description,
                toTaskProperties(data.task()),
                toQuestionProperties(data.question()),
                toIdeaProperties(data.idea()),
                toDecisionProperties(data.decision()));
    }

    private TaskProperties toTaskProperties(TaskData data) {
        if (data == null) {
            return null;
        }
        return new TaskProperties(
                data.status(), data.progress(), data.priority(), data.progressMode(),
                parseDate(data.startDate()), parseDate(data.dueDate()));
    }

    private QuestionProperties toQuestionProperties(QuestionData data) {
        return data == null ? null : new QuestionProperties(data.status(), data.resolution());
    }

    private IdeaProperties toIdeaProperties(IdeaData data) {
        return data == null ? null : new IdeaProperties(data.status());
    }

    private DecisionProperties toDecisionProperties(DecisionData data) {
        return data == null ? null : new DecisionProperties(
                data.decision(), data.rationale(), parseDateTime(data.decidedAt()));
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private void validateAcyclic(BoardDocument document, RelationType relationType)
            throws InvalidBoardFileException {
        for (BoardElement start : document.elements()) {
            Queue<UUID> queue = new ArrayDeque<>();
            Set<UUID> visited = new HashSet<>();
            queue.add(start.id());
            while (!queue.isEmpty()) {
                UUID current = queue.remove();
                for (Connection connection : document.connections()) {
                    if (connection.relationType() != relationType
                            || !connection.sourceElementId().equals(current)) {
                        continue;
                    }
                    UUID target = connection.targetElementId();
                    if (target.equals(start.id())) {
                        throw new InvalidBoardFileException(
                                relationType + "に循環が含まれています。");
                    }
                    if (visited.add(target)) {
                        queue.add(target);
                    }
                }
            }
        }
    }

    /** 保存直前にドキュメントを再構築して整合性を検証します。 */
    public void validateDocument(BoardDocument document) throws InvalidBoardFileException {
        Objects.requireNonNull(document, "document");
        toDocument(JsonBoardRepository.toFileData(document));
    }

    private static void require(boolean condition, String message)
            throws InvalidBoardFileException {
        if (!condition) {
            throw new InvalidBoardFileException(message);
        }
    }
}
