package org.takoyaki.taskboardmanager.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.takoyaki.taskboardmanager.core.BoardClipboard;
import org.takoyaki.taskboardmanager.core.BoardCommandHistory;
import org.takoyaki.taskboardmanager.core.ConnectionService;
import org.takoyaki.taskboardmanager.core.HierarchicalLayoutEngine;
import org.takoyaki.taskboardmanager.core.LayoutRequest;
import org.takoyaki.taskboardmanager.core.TaskProgressService;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.DocumentChange;
import org.takoyaki.taskboardmanager.model.ElementSize;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.Priority;
import org.takoyaki.taskboardmanager.model.ProgressMode;
import org.takoyaki.taskboardmanager.model.Position;
import org.takoyaki.taskboardmanager.model.RelationType;
import org.takoyaki.taskboardmanager.model.TaskProperties;
import org.takoyaki.taskboardmanager.model.TaskStatus;
import org.takoyaki.taskboardmanager.model.ViewSettings;
import org.takoyaki.taskboardmanager.persistence.InvalidBoardFileException;
import org.takoyaki.taskboardmanager.persistence.JsonBoardRepository;

/** MVP 1.0互換性と1.1〜1.8の中核ロジックを確認するテストです。 */
class MvpIntegrityTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void 要素削除の取り消しで関連接続も復元する() {
        BoardDocument document = connectedDocument();
        UUID deletingId = document.elements().iterator().next().id();
        BoardCommandHistory history = new BoardCommandHistory(document);

        history.execute("削除", () -> document.removeElement(deletingId));
        assertEquals(1, document.elements().size());
        assertTrue(document.connections().isEmpty());

        history.undo();
        assertEquals(2, document.elements().size());
        assertEquals(1, document.connections().size());
    }

    @Test
    void コピーした要素と内部接続へ新しいUUIDを割り当てる() {
        BoardDocument document = connectedDocument();
        Set<UUID> originalIds = document.elements().stream()
                .map(element -> element.id())
                .collect(java.util.stream.Collectors.toSet());
        BoardClipboard clipboard = new BoardClipboard();
        clipboard.copy(document, originalIds);

        Set<UUID> pastedIds = clipboard.paste(document);

        assertEquals(2, pastedIds.size());
        assertTrue(java.util.Collections.disjoint(originalIds, pastedIds));
        assertEquals(4, document.elements().size());
        assertEquals(2, document.connections().size());
        Connection pastedConnection = document.connections().stream()
                .filter(connection -> pastedIds.contains(connection.sourceElementId()))
                .findFirst()
                .orElseThrow();
        assertTrue(pastedIds.contains(pastedConnection.targetElementId()));
    }

    @Test
    void JSON保存と読込で論理状態を維持する() throws Exception {
        BoardDocument document = connectedDocument();
        document.setViewSettings(new ViewSettings(1.5, 120.0, -80.0));
        JsonBoardRepository repository = new JsonBoardRepository();
        Path path = temporaryDirectory.resolve("roundtrip.tbm.json");

        repository.save(document, path);
        BoardDocument loaded = repository.load(path);

        assertEquals(document.snapshot(), loaded.snapshot());
    }

    @Test
    void schema2で種類別属性と関係種別を往復保存する() throws Exception {
        BoardDocument document = new BoardDocument(UUID.randomUUID(), "typed");
        NodeElement parent = new NodeElement(
                UUID.randomUUID(), "goal", new Position(0.0, 0.0),
                new ElementSize(180.0, 76.0), NodeType.GOAL,
                "目標", null, null, null, null);
        NodeElement task = new NodeElement(
                UUID.randomUUID(), "task", new Position(220.0, 0.0),
                new ElementSize(180.0, 76.0), NodeType.TASK, "作業",
                new TaskProperties(
                        TaskStatus.IN_PROGRESS, 45, Priority.HIGH, ProgressMode.MANUAL,
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
                null, null, null);
        document.addElement(parent);
        document.addElement(task);
        document.addConnection(new Connection(
                UUID.randomUUID(), parent.id(), task.id(), RelationType.PARENT_CHILD));
        Path path = temporaryDirectory.resolve("typed.tbm.json");

        JsonBoardRepository repository = new JsonBoardRepository();
        repository.save(document, path);

        assertEquals(document.snapshot(), repository.load(path).snapshot());
    }

    @Test
    void 不正な接続先を含むJSONを拒否する() throws Exception {
        Path path = temporaryDirectory.resolve("invalid.tbm.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 1,
                  "documentId": "00000000-0000-0000-0000-000000000001",
                  "title": "invalid",
                  "elements": [],
                  "connections": [{
                    "id": "00000000-0000-0000-0000-000000000002",
                    "sourceElementId": "00000000-0000-0000-0000-000000000003",
                    "targetElementId": "00000000-0000-0000-0000-000000000004"
                  }],
                  "viewSettings": {"zoom": 1.0, "offsetX": 0.0, "offsetY": 0.0}
                }
                """);

        JsonBoardRepository repository = new JsonBoardRepository();
        assertThrows(InvalidBoardFileException.class, () -> repository.load(path));
    }

    @Test
    void Undo後の新規編集でRedo履歴を破棄する() {
        BoardDocument document = connectedDocument();
        BoardCommandHistory history = new BoardCommandHistory(document);
        history.execute("名称1", () -> document.setTitle("名称1"));
        history.undo();
        assertTrue(history.canRedo());

        history.execute("名称2", () -> document.setTitle("名称2"));

        assertFalse(history.canRedo());
        assertNotEquals("名称1", document.title());
    }

    @Test
    void 旧schema1を既定の種類へ移行して読み込む() throws Exception {
        Path path = temporaryDirectory.resolve("legacy.tbm.json");
        Files.writeString(path, """
                {
                  "schemaVersion": 1,
                  "documentId": "00000000-0000-0000-0000-000000000001",
                  "title": "legacy",
                  "elements": [{
                    "id": "00000000-0000-0000-0000-000000000002",
                    "kind": "NODE", "title": "old", "x": 0, "y": 0,
                    "width": 180, "height": 76
                  }],
                  "connections": [],
                  "viewSettings": {"zoom": 1.0, "offsetX": 0.0, "offsetY": 0.0}
                }
                """);

        BoardDocument loaded = new JsonBoardRepository().load(path);
        NodeElement node = (NodeElement) loaded.elements().iterator().next();

        assertEquals(NodeType.GENERAL, node.nodeType());
        assertEquals("", node.description());
    }

    @Test
    void 親子関係と依存関係の循環を拒否する() {
        BoardDocument document = threeNodeDocument();
        NodeElement[] nodes = document.elements().stream()
                .map(NodeElement.class::cast)
                .toArray(NodeElement[]::new);
        ConnectionService service = new ConnectionService(document);
        service.createConnection(nodes[0].id(), nodes[1].id(), RelationType.PARENT_CHILD);
        service.createConnection(nodes[1].id(), nodes[2].id(), RelationType.PARENT_CHILD);

        assertThrows(IllegalArgumentException.class, () -> service.createConnection(
                nodes[2].id(), nodes[0].id(), RelationType.PARENT_CHILD));

        service.createConnection(nodes[0].id(), nodes[2].id(), RelationType.DEPENDS_ON);
        assertThrows(IllegalArgumentException.class, () -> service.createConnection(
                nodes[2].id(), nodes[0].id(), RelationType.DEPENDS_ON));
    }

    @Test
    void 階層レイアウトは決定的で元の座標を変更しない() {
        BoardDocument document = threeNodeDocument();
        NodeElement[] nodes = document.elements().stream()
                .map(NodeElement.class::cast)
                .toArray(NodeElement[]::new);
        new ConnectionService(document).createConnection(
                nodes[0].id(), nodes[1].id(), RelationType.PARENT_CHILD);
        var original = document.snapshot();
        LayoutRequest request = new LayoutRequest(
                document.elements().stream().map(NodeElement.class::cast).toList(),
                document.connections().stream().toList(), 100.0, 40.0);
        HierarchicalLayoutEngine engine = new HierarchicalLayoutEngine();

        assertEquals(engine.layout(request), engine.layout(request));
        assertEquals(original, document.snapshot());
    }

    @Test
    void 親タスク進捗を直接の子タスク平均から集計する() {
        BoardDocument document = new BoardDocument(UUID.randomUUID(), "tasks");
        NodeElement parent = taskNode("parent", 0);
        NodeElement first = taskNode("first", 20);
        NodeElement second = taskNode("second", 80);
        document.addElement(parent);
        document.addElement(first);
        document.addElement(second);
        ConnectionService service = new ConnectionService(document);
        service.createConnection(parent.id(), first.id(), RelationType.PARENT_CHILD);
        service.createConnection(parent.id(), second.id(), RelationType.PARENT_CHILD);

        assertEquals(50, new TaskProgressService().childAverage(document, parent.id()));
    }

    @Test
    void 変更イベントとリビジョンを実変更だけで更新する() {
        BoardDocument document = new BoardDocument(UUID.randomUUID(), "events");
        ArrayList<DocumentChange> changes = new ArrayList<>();
        document.addChangeListener(changes::add);
        NodeElement node = new NodeElement(
                UUID.randomUUID(), "node", new Position(0.0, 0.0),
                new ElementSize(180.0, 76.0));

        document.addElement(node);
        document.updateElement(node);

        assertEquals(1, document.revision());
        assertEquals(1, changes.size());
        assertEquals(DocumentChange.ChangeKind.ELEMENT_ADDED, changes.getFirst().kind());
    }

    private BoardDocument connectedDocument() {
        BoardDocument document = new BoardDocument(UUID.randomUUID(), "テストボード");
        NodeElement first = new NodeElement(
                UUID.randomUUID(), "A", new Position(10.0, 20.0), new ElementSize(180.0, 76.0));
        NodeElement second = new NodeElement(
                UUID.randomUUID(), "B", new Position(250.0, 80.0), new ElementSize(180.0, 76.0));
        document.addElement(first);
        document.addElement(second);
        document.addConnection(new Connection(UUID.randomUUID(), first.id(), second.id()));
        return document;
    }

    private BoardDocument threeNodeDocument() {
        BoardDocument document = new BoardDocument(UUID.randomUUID(), "three");
        for (int index = 0; index < 3; index++) {
            document.addElement(new NodeElement(
                    UUID.randomUUID(), "N" + index,
                    new Position(index * 200.0, 0.0), new ElementSize(180.0, 76.0)));
        }
        return document;
    }

    private NodeElement taskNode(String title, int progress) {
        return new NodeElement(
                UUID.randomUUID(), title, new Position(0.0, 0.0), new ElementSize(180.0, 76.0),
                NodeType.TASK, "", new TaskProperties(
                        TaskStatus.IN_PROGRESS, progress, Priority.NORMAL,
                        ProgressMode.MANUAL, null, null), null, null, null);
    }
}
