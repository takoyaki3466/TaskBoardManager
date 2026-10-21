package org.takoyaki.taskboardmanager.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Objects;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.ConnectionData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.DecisionData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.ElementData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.ElementKind;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.IdeaData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.QuestionData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.TaskData;
import org.takoyaki.taskboardmanager.persistence.BoardFileData.ViewSettingsData;

/** `.tbm.json`ファイルを安全に保存・読み込みします。 */
public final class JsonBoardRepository {

    public static final String FILE_SUFFIX = ".tbm.json";

    private final ObjectMapper objectMapper;
    private final DocumentValidator validator;

    public JsonBoardRepository() {
        objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        validator = new DocumentValidator();
    }

    /** 完全に解析・検証された新しいドキュメントを返します。 */
    public BoardDocument load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try {
            BoardFileData data = objectMapper.readValue(path.toFile(), BoardFileData.class);
            return validator.toDocument(data);
        } catch (JsonProcessingException exception) {
            throw new InvalidBoardFileException(
                    "JSONを解析できません: " + exception.getOriginalMessage(), exception);
        }
    }

    /**
     * 一時ファイルへ書き込んだ後、可能なら原子的に保存先を置換します。
     * 原子的移動が非対応の場合のみ通常の置換へフォールバックします。
     */
    public void save(BoardDocument document, Path requestedPath) throws IOException {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(requestedPath, "requestedPath");
        validator.validateDocument(document);

        Path path = requestedPath.toAbsolutePath().normalize();
        Path parent = path.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("保存先ディレクトリが存在しません。");
        }
        Path temporaryPath = Files.createTempFile(parent, ".taskboardmanager-", ".tmp");
        try {
            objectMapper.writeValue(temporaryPath.toFile(), toFileData(document));
            try {
                Files.move(
                        temporaryPath,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    static BoardFileData toFileData(BoardDocument document) {
        List<ElementData> elements = document.elements().stream()
                .map(JsonBoardRepository::toElementData)
                .toList();
        List<ConnectionData> connections = document.connections().stream()
                .map(JsonBoardRepository::toConnectionData)
                .toList();
        return new BoardFileData(
                DocumentValidator.CURRENT_SCHEMA_VERSION,
                document.documentId(),
                document.title(),
                elements,
                connections,
                new ViewSettingsData(
                        document.viewSettings().zoom(),
                        document.viewSettings().offsetX(),
                        document.viewSettings().offsetY()));
    }

    private static ElementData toElementData(BoardElement element) {
        ElementKind kind = element instanceof BoxElement ? ElementKind.BOX : ElementKind.NODE;
        NodeElement node = element instanceof NodeElement nodeElement ? nodeElement : null;
        return new ElementData(
                element.id(),
                kind,
                element.title(),
                element.position().x(),
                element.position().y(),
                element.size().width(),
                element.size().height(),
                node == null ? null : node.nodeType(),
                node == null ? null : node.description(),
                node == null || node.taskProperties() == null
                        ? null : new TaskData(
                                node.taskProperties().status(),
                                node.taskProperties().progress(),
                                node.taskProperties().priority(),
                                node.taskProperties().progressMode(),
                                format(node.taskProperties().startDate()),
                                format(node.taskProperties().dueDate())),
                node == null || node.questionProperties() == null
                        ? null : new QuestionData(
                                node.questionProperties().status(),
                                node.questionProperties().resolution()),
                node == null || node.ideaProperties() == null
                        ? null : new IdeaData(node.ideaProperties().status()),
                node == null || node.decisionProperties() == null
                        ? null : new DecisionData(
                                node.decisionProperties().decision(),
                                node.decisionProperties().rationale(),
                                format(node.decisionProperties().decidedAt())));
    }

    private static ConnectionData toConnectionData(Connection connection) {
        return new ConnectionData(
                connection.id(),
                connection.sourceElementId(),
                connection.targetElementId(),
                connection.relationType());
    }

    private static String format(TemporalAccessor temporal) {
        return temporal == null ? null : temporal.toString();
    }
}
