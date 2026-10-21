package org.takoyaki.taskboardmanager.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.Position;

/** 選択要素と内部接続をアプリ内コピー用に保持します。 */
public final class BoardClipboard {

    private static final double PASTE_OFFSET = 28.0;

    private List<BoardElement> elements = List.of();
    private List<Connection> connections = List.of();
    private int pasteCount;

    public void copy(BoardDocument document, Set<UUID> selectedElementIds) {
        Set<UUID> ids = Set.copyOf(selectedElementIds);
        elements = document.elements().stream()
                .filter(element -> ids.contains(element.id()))
                .toList();
        connections = document.connections().stream()
                .filter(connection -> ids.contains(connection.sourceElementId())
                        && ids.contains(connection.targetElementId()))
                .toList();
        pasteCount = 0;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * 新しいUUIDへ対応付けた要素と内部接続を追加します。
     *
     * @return 追加された要素ID
     */
    public Set<UUID> paste(BoardDocument document) {
        if (isEmpty()) {
            return Set.of();
        }
        pasteCount++;
        double offset = PASTE_OFFSET * pasteCount;
        Map<UUID, UUID> idMapping = new HashMap<>();
        List<BoardElement> pastedElements = new ArrayList<>();
        for (BoardElement source : elements) {
            UUID newId = UUID.randomUUID();
            idMapping.put(source.id(), newId);
            Position newPosition = new Position(
                    source.position().x() + offset,
                    source.position().y() + offset);
            BoardElement pasted = switch (source) {
                case NodeElement node -> new NodeElement(
                        newId, node.title(), newPosition, node.size(),
                        node.nodeType(), node.description(), node.taskProperties(),
                        node.questionProperties(), node.ideaProperties(),
                        node.decisionProperties());
                case BoxElement box -> new BoxElement(
                        newId, box.title(), newPosition, box.size());
            };
            document.addElement(pasted);
            pastedElements.add(pasted);
        }
        for (Connection source : connections) {
            document.addConnection(new Connection(
                    UUID.randomUUID(),
                    idMapping.get(source.sourceElementId()),
                    idMapping.get(source.targetElementId()),
                    source.relationType()));
        }
        Set<UUID> pastedIds = new HashSet<>();
        for (BoardElement pasted : pastedElements) {
            pastedIds.add(pasted.id());
        }
        return Set.copyOf(pastedIds);
    }
}
