package org.takoyaki.taskboardmanager.desktop.view;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import org.takoyaki.taskboardmanager.core.BoardClipboard;
import org.takoyaki.taskboardmanager.core.BoardCommandHistory;
import org.takoyaki.taskboardmanager.core.BoardConfiguration;
import org.takoyaki.taskboardmanager.core.BoardService;
import org.takoyaki.taskboardmanager.core.ConnectionService;
import org.takoyaki.taskboardmanager.core.HierarchicalLayoutEngine;
import org.takoyaki.taskboardmanager.core.LayoutRequest;
import org.takoyaki.taskboardmanager.core.LayoutResult;
import org.takoyaki.taskboardmanager.core.TaskProgressService;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoardSnapshot;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.DecisionProperties;
import org.takoyaki.taskboardmanager.model.ElementSize;
import org.takoyaki.taskboardmanager.model.Position;
import org.takoyaki.taskboardmanager.model.IdeaProperties;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.QuestionProperties;
import org.takoyaki.taskboardmanager.model.RelationType;
import org.takoyaki.taskboardmanager.model.TaskProperties;

/**
 * 無限平面の描画と編集操作を統括し、変更をUndo/Redo可能な単位へまとめます。
 */
public final class BoardCanvas extends Pane {

    private static final Color GRID_COLOR = Color.web("#3b4352", 0.62);
    private static final double VIEW_MARGIN = 72.0;

    private final Canvas backgroundLayer = new Canvas();
    private final Group worldViewport = new Group();
    private final Group worldRoot = new Group();
    private final Group connectionLayer = new Group();
    private final Group elementLayer = new Group();
    private final Pane screenOverlay = new Pane();
    private final Line connectionPreview = new Line();
    private final Rectangle selectionRectangle = new Rectangle();
    private final Label emptyHint = new Label("ツールバーの「ノード」または「ボックス」から始めましょう");
    private final ViewportTransform viewportTransform = new ViewportTransform();
    private final BoardClipboard clipboard = new BoardClipboard();
    private final TaskProgressService taskProgressService = new TaskProgressService();
    private final Map<UUID, BoardElementView> elementViews = new LinkedHashMap<>();
    private final Map<UUID, ConnectionView> connectionViews = new LinkedHashMap<>();
    private final Set<UUID> selectedElementIds = new LinkedHashSet<>();
    private final Map<UUID, Position> dragStartPositions = new LinkedHashMap<>();

    private BoardDocument document;
    private BoardService boardService;
    private ConnectionService connectionService;
    private BoardCommandHistory commandHistory;
    private InteractionState interactionState = InteractionState.IDLE;
    private UUID selectedConnectionId;
    private UUID connectionSourceId;
    private UUID connectionTargetId;
    private Point2D gestureStart = Point2D.ZERO;
    private Point2D lastPanPoint = Point2D.ZERO;
    private ElementSize resizeStartSize;
    private BoardSnapshot interactionStartSnapshot;
    private boolean spacePressed;

    private Consumer<SelectionInfo> selectionListener = selection -> { };
    private Consumer<BoardElement> editRequestHandler = element -> { };
    private Consumer<String> statusListener = status -> { };
    private DoubleConsumer zoomListener = zoom -> { };
    private Runnable changeListener = () -> { };

    public BoardCanvas() {
        getStyleClass().add("board-canvas");
        setFocusTraversable(true);
        setPickOnBounds(true);
        connectionLayer.setPickOnBounds(false);
        elementLayer.setPickOnBounds(false);
        worldRoot.getChildren().addAll(connectionLayer, elementLayer);
        worldViewport.getChildren().add(worldRoot);
        worldViewport.getTransforms().add(viewportTransform.affine());

        connectionPreview.getStyleClass().add("connection-preview");
        connectionPreview.setVisible(false);
        selectionRectangle.getStyleClass().add("selection-rectangle");
        selectionRectangle.setVisible(false);
        emptyHint.getStyleClass().add("empty-canvas-hint");
        emptyHint.setManaged(false);
        screenOverlay.setMouseTransparent(true);
        screenOverlay.getChildren().addAll(connectionPreview, selectionRectangle, emptyHint);
        getChildren().addAll(backgroundLayer, worldViewport, screenOverlay);
        installCanvasHandlers();
    }

    /** 新しいドキュメントへ安全に表示と履歴を切り替えます。 */
    public void setDocument(BoardDocument document) {
        this.document = Objects.requireNonNull(document, "document");
        boardService = new BoardService(document);
        connectionService = new ConnectionService(document);
        commandHistory = new BoardCommandHistory(document);
        viewportTransform.apply(document.viewSettings());
        clearSelectionState();
        rebuildViews();
        notifySelection();
        notifyViewportChanged(false);
    }

    public BoardDocument document() {
        ensureDocument();
        return document;
    }

    public void setSelectionListener(Consumer<SelectionInfo> listener) {
        selectionListener = Objects.requireNonNull(listener, "listener");
    }

    public void setEditRequestHandler(Consumer<BoardElement> handler) {
        editRequestHandler = Objects.requireNonNull(handler, "handler");
    }

    public void setStatusListener(Consumer<String> listener) {
        statusListener = Objects.requireNonNull(listener, "listener");
    }

    public void setZoomListener(DoubleConsumer listener) {
        zoomListener = Objects.requireNonNull(listener, "listener");
    }

    public void setChangeListener(Runnable listener) {
        changeListener = Objects.requireNonNull(listener, "listener");
    }

    /** 現在の表示中央にノードを追加し、1回の履歴として記録します。 */
    public void addNodeAtCenter() {
        ensureDocument();
        Point2D center = viewportTransform.screenToWorld(getWidth() / 2.0, getHeight() / 2.0);
        BoardElement[] created = new BoardElement[1];
        commandHistory.execute("ノード追加", () -> created[0] = boardService.createNode(
                "新しいノード",
                centeredPosition(center, BoardConfiguration.DEFAULT_NODE_SIZE)));
        rebuildViews();
        selectOnly(created[0].id());
        commitStatus("ノードを追加しました");
    }

    /** 現在の表示中央にボックスを追加し、1回の履歴として記録します。 */
    public void addBoxAtCenter() {
        ensureDocument();
        Point2D center = viewportTransform.screenToWorld(getWidth() / 2.0, getHeight() / 2.0);
        BoardElement[] created = new BoardElement[1];
        commandHistory.execute("ボックス追加", () -> created[0] = boardService.createBox(
                "新しいボックス",
                centeredPosition(center, BoardConfiguration.DEFAULT_BOX_SIZE)));
        rebuildViews();
        selectOnly(created[0].id());
        commitStatus("ボックスを追加しました");
    }

    /** 単一選択中の要素をインスペクター入力で更新します。 */
    public void updateSelectedElement(
            String title, double x, double y, Double width, Double height) {
        if (selectedElementIds.size() != 1) {
            return;
        }
        UUID id = selectedElementIds.iterator().next();
        commandHistory.execute("要素のプロパティ変更", () -> {
            boardService.renameElement(id, title);
            boardService.moveElement(id, new Position(x, y));
            BoardElement updated = requireElement(id);
            if (updated instanceof BoxElement && width != null && height != null) {
                boardService.resizeElement(id, new ElementSize(width, height));
            }
        });
        rebuildViews();
        applySelectionStyles();
        notifySelection();
        commitStatus("プロパティを変更しました");
    }

    public void renameBoard(String title) {
        commandHistory.execute("ボード名変更", () -> document.setTitle(title));
        commitStatus("ボード名を変更しました");
    }

    /** 単一選択ノードの種類と詳細属性を履歴付きで更新します。 */
    public void updateSelectedNodeDetails(
            NodeType nodeType,
            String description,
            TaskProperties taskProperties,
            QuestionProperties questionProperties,
            IdeaProperties ideaProperties,
            DecisionProperties decisionProperties) {
        if (selectedElementIds.size() != 1) {
            return;
        }
        UUID id = selectedElementIds.iterator().next();
        commandHistory.execute("ノード詳細変更", () -> {
            boardService.updateNodeDetails(
                    id, nodeType, description, taskProperties,
                    questionProperties, ideaProperties, decisionProperties);
            taskProgressService.refreshChildAverages(document);
        });
        rebuildViews();
        applySelectionStyles();
        notifySelection();
        commitStatus("ノード詳細を変更しました");
    }

    /** インスペクターの基本項目とノード固有項目を1回のUndo単位で更新します。 */
    public void updateSelectedNode(
            String title,
            double x,
            double y,
            NodeType nodeType,
            String description,
            TaskProperties taskProperties,
            QuestionProperties questionProperties,
            IdeaProperties ideaProperties,
            DecisionProperties decisionProperties) {
        if (selectedElementIds.size() != 1) {
            return;
        }
        UUID id = selectedElementIds.iterator().next();
        commandHistory.execute("ノードのプロパティ変更", () -> {
            boardService.renameElement(id, title);
            boardService.moveElement(id, new Position(x, y));
            boardService.updateNodeDetails(
                    id, nodeType, description, taskProperties,
                    questionProperties, ideaProperties, decisionProperties);
            taskProgressService.refreshChildAverages(document);
        });
        rebuildViews();
        applySelectionStyles();
        notifySelection();
        commitStatus("ノードのプロパティを変更しました");
    }

    /** 選択接続の関係種別を循環検証後に更新します。 */
    public void updateSelectedConnectionRelation(RelationType relationType) {
        if (selectedConnectionId == null) {
            return;
        }
        commandHistory.execute("接続関係変更", () -> {
            connectionService.updateRelation(selectedConnectionId, relationType);
            taskProgressService.refreshChildAverages(document);
        });
        rebuildViews();
        applySelectionStyles();
        notifySelection();
        commitStatus("接続関係を変更しました");
    }

    /** 検索結果などから要素を選択し、現在の自由配置座標へフォーカスします。 */
    public void selectAndFocus(UUID elementId) {
        if (document.findElement(elementId).isEmpty()) {
            return;
        }
        selectOnly(elementId);
        focusSelection();
    }

    /** 別表示から同一ドキュメントの要素を選択します。 */
    public void selectElement(UUID elementId) {
        if (document.findElement(elementId).isPresent()) {
            selectOnly(elementId);
        }
    }

    /** 自動レイアウト結果を明示的に自由配置へ反映し、Undo可能にします。 */
    public void applyHierarchicalLayout() {
        List<NodeElement> nodes = document.elements().stream()
                .filter(NodeElement.class::isInstance)
                .map(NodeElement.class::cast)
                .toList();
        LayoutResult result = new HierarchicalLayoutEngine().layout(new LayoutRequest(
                nodes,
                document.connections().stream().toList(),
                BoardConfiguration.HIERARCHY_HORIZONTAL_GAP,
                BoardConfiguration.HIERARCHY_VERTICAL_GAP));
        commandHistory.execute("階層レイアウトを配置へ反映", () -> {
            for (Map.Entry<UUID, Position> entry : result.positions().entrySet()) {
                boardService.moveElement(entry.getKey(), entry.getValue());
            }
        });
        rebuildViews();
        showAllElements();
        commitStatus("階層レイアウトを自由配置へ反映しました");
    }

    public void undo() {
        String description = commandHistory.undo();
        if (description.isEmpty()) {
            statusListener.accept("取り消せる操作はありません");
            return;
        }
        restoreAfterHistory("元に戻す: " + description);
    }

    public void redo() {
        String description = commandHistory.redo();
        if (description.isEmpty()) {
            statusListener.accept("やり直せる操作はありません");
            return;
        }
        restoreAfterHistory("やり直す: " + description);
    }

    public boolean canUndo() {
        return commandHistory != null && commandHistory.canUndo();
    }

    public boolean canRedo() {
        return commandHistory != null && commandHistory.canRedo();
    }

    public void copySelection() {
        clipboard.copy(document, selectedElementIds);
        statusListener.accept(clipboard.isEmpty() ? "コピー対象がありません" : "選択要素をコピーしました");
    }

    public void paste() {
        if (clipboard.isEmpty()) {
            statusListener.accept("貼り付ける要素がありません");
            return;
        }
        AtomicReference<Set<UUID>> pasted = new AtomicReference<>(Set.of());
        commandHistory.execute("貼り付け", () -> pasted.set(clipboard.paste(document)));
        rebuildViews();
        selectElements(pasted.get());
        commitStatus("要素を貼り付けました");
    }

    public void cutSelection() {
        if (selectedElementIds.isEmpty()) {
            return;
        }
        copySelection();
        deleteSelection();
    }

    public void selectAllElements() {
        selectedConnectionId = null;
        selectedElementIds.clear();
        for (BoardElement element : document.elements()) {
            selectedElementIds.add(element.id());
        }
        applySelectionStyles();
        notifySelection();
        statusListener.accept("すべての要素を選択しました");
    }

    /** 選択要素または接続を1回の操作として削除します。 */
    public void deleteSelection() {
        if (selectedElementIds.isEmpty() && selectedConnectionId == null) {
            return;
        }
        Set<UUID> deletingElements = Set.copyOf(selectedElementIds);
        UUID deletingConnection = selectedConnectionId;
        commandHistory.execute("選択対象の削除", () -> {
            if (deletingConnection != null) {
                connectionService.deleteConnection(deletingConnection);
            }
            for (UUID elementId : deletingElements) {
                boardService.deleteElement(elementId);
            }
            taskProgressService.refreshChildAverages(document);
        });
        clearSelectionState();
        rebuildViews();
        notifySelection();
        commitStatus("選択対象を削除しました");
    }

    public void resetZoom() {
        viewportTransform.set(1.0, viewportTransform.offsetX(), viewportTransform.offsetY());
        notifyViewportChanged(true);
    }

    public void showAllElements() {
        focusElements(document.elements().stream().toList());
    }

    public void focusSelection() {
        List<BoardElement> selected = selectedElements();
        if (selected.isEmpty()) {
            statusListener.accept("フォーカスする要素がありません");
            return;
        }
        focusElements(selected);
    }

    public void showConnectionHint() {
        statusListener.accept("要素右側の青いハンドルから接続先へドラッグします");
    }

    @Override
    protected void layoutChildren() {
        backgroundLayer.setWidth(getWidth());
        backgroundLayer.setHeight(getHeight());
        screenOverlay.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
        emptyHint.autosize();
        emptyHint.relocate(
                (getWidth() - emptyHint.getWidth()) / 2.0,
                (getHeight() - emptyHint.getHeight()) / 2.0);
        drawGrid();
    }

    private void installCanvasHandlers() {
        setOnMousePressed(this::handleCanvasPressed);
        setOnMouseDragged(this::handleCanvasDragged);
        setOnMouseReleased(this::handleCanvasReleased);
        setOnScroll(event -> {
            if (interactionState != InteractionState.IDLE) {
                return;
            }
            double factor = event.getDeltaY() >= 0.0
                    ? BoardConfiguration.ZOOM_STEP
                    : 1.0 / BoardConfiguration.ZOOM_STEP;
            viewportTransform.zoomAt(
                    viewportTransform.zoom() * factor, event.getX(), event.getY());
            notifyViewportChanged(true);
            event.consume();
        });
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                spacePressed = true;
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                deleteSelection();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                cancelTemporaryOperation();
                event.consume();
            } else if (event.isShortcutDown()) {
                handleEditingShortcut(event);
            }
        });
        setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                spacePressed = false;
                event.consume();
            }
        });
    }

    private void handleEditingShortcut(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == KeyCode.Z && event.isShiftDown()) {
            redo();
        } else if (event.getCode() == KeyCode.Z) {
            undo();
        } else if (event.getCode() == KeyCode.Y) {
            redo();
        } else if (event.getCode() == KeyCode.C) {
            copySelection();
        } else if (event.getCode() == KeyCode.V) {
            paste();
        } else if (event.getCode() == KeyCode.X) {
            cutSelection();
        } else if (event.getCode() == KeyCode.A) {
            selectAllElements();
        } else {
            return;
        }
        event.consume();
    }

    private void handleCanvasPressed(MouseEvent event) {
        requestFocus();
        if (interactionState != InteractionState.IDLE) {
            return;
        }
        boolean panRequested = event.getButton() == MouseButton.MIDDLE
                || (spacePressed && event.getButton() == MouseButton.PRIMARY);
        if (panRequested) {
            interactionState = InteractionState.PANNING_CANVAS;
            lastPanPoint = new Point2D(event.getX(), event.getY());
            statusListener.accept("キャンバスを移動中");
        } else if (event.getButton() == MouseButton.PRIMARY
                && !isElementOrConnection(event.getTarget())) {
            interactionState = InteractionState.SELECTING_RANGE;
            gestureStart = new Point2D(event.getX(), event.getY());
            selectionRectangle.setX(event.getX());
            selectionRectangle.setY(event.getY());
            selectionRectangle.setWidth(0.0);
            selectionRectangle.setHeight(0.0);
            selectionRectangle.setVisible(true);
            if (!event.isShortcutDown()) {
                clearSelectionState();
                applySelectionStyles();
                notifySelection();
            }
        }
        event.consume();
    }

    private void handleCanvasDragged(MouseEvent event) {
        if (interactionState == InteractionState.PANNING_CANVAS) {
            Point2D current = new Point2D(event.getX(), event.getY());
            viewportTransform.pan(
                    current.getX() - lastPanPoint.getX(), current.getY() - lastPanPoint.getY());
            lastPanPoint = current;
            notifyViewportChanged(true);
        } else if (interactionState == InteractionState.SELECTING_RANGE) {
            updateSelectionRectangle(event.getX(), event.getY());
        }
        event.consume();
    }

    private void handleCanvasReleased(MouseEvent event) {
        if (interactionState == InteractionState.PANNING_CANVAS) {
            interactionState = InteractionState.IDLE;
            statusListener.accept("選択ツール");
        } else if (interactionState == InteractionState.SELECTING_RANGE) {
            completeRangeSelection(event.isShortcutDown());
            interactionState = InteractionState.IDLE;
        }
        event.consume();
    }

    private void addElementView(BoardElement element) {
        BoardElementView view = ElementViewFactory.create(element);
        elementViews.put(element.id(), view);
        elementLayer.getChildren().add(view);
        installElementHandlers(view);
    }

    private void installElementHandlers(BoardElementView view) {
        view.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || interactionState != InteractionState.IDLE
                    || spacePressed) {
                return;
            }
            requestFocus();
            if (event.isShortcutDown()) {
                toggleElementSelection(view.elementId());
                event.consume();
                return;
            }
            if (!selectedElementIds.contains(view.elementId())) {
                selectOnly(view.elementId());
            }
            if (event.getClickCount() == 2 && selectedElementIds.size() == 1) {
                interactionState = InteractionState.EDITING_TEXT;
                try {
                    editRequestHandler.accept(requireElement(view.elementId()));
                } finally {
                    interactionState = InteractionState.IDLE;
                }
                event.consume();
                return;
            }
            Point2D worldPoint = toWorld(event);
            gestureStart = worldPoint;
            dragStartPositions.clear();
            for (UUID id : selectedElementIds) {
                dragStartPositions.put(id, requireElement(id).position());
            }
            interactionStartSnapshot = document.snapshot();
            interactionState = InteractionState.DRAGGING_ELEMENT;
            event.consume();
        });
        view.setOnMouseDragged(event -> {
            if (interactionState != InteractionState.DRAGGING_ELEMENT
                    || !selectedElementIds.contains(view.elementId())) {
                return;
            }
            Point2D current = toWorld(event);
            double deltaX = current.getX() - gestureStart.getX();
            double deltaY = current.getY() - gestureStart.getY();
            for (Map.Entry<UUID, Position> entry : dragStartPositions.entrySet()) {
                Position start = entry.getValue();
                BoardElement updated = boardService.moveElement(entry.getKey(), new Position(
                        start.x() + deltaX, start.y() + deltaY));
                elementViews.get(entry.getKey()).update(updated);
                updateRelatedConnections(entry.getKey());
            }
            notifySelection();
            event.consume();
        });
        view.setOnMouseReleased(event -> {
            if (interactionState == InteractionState.DRAGGING_ELEMENT) {
                commandHistory.recordAppliedChange("要素移動", interactionStartSnapshot);
                finishInteraction("要素を移動しました", true);
                event.consume();
            }
        });
        installConnectionHandleHandlers(view);
        if (view instanceof BoxElementView boxView) {
            installResizeHandlers(boxView);
        }
    }

    private void installConnectionHandleHandlers(BoardElementView sourceView) {
        Node handle = sourceView.connectionHandle();
        handle.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || interactionState != InteractionState.IDLE
                    || spacePressed) {
                return;
            }
            interactionState = InteractionState.CONNECTING_ELEMENTS;
            connectionSourceId = sourceView.elementId();
            Point2D start = connectionHandleScreenPoint(sourceView.elementId());
            connectionPreview.setStartX(start.getX());
            connectionPreview.setStartY(start.getY());
            connectionPreview.setEndX(start.getX());
            connectionPreview.setEndY(start.getY());
            connectionPreview.setVisible(true);
            statusListener.accept("接続先を選択してください");
            event.consume();
        });
        handle.setOnMouseDragged(event -> {
            if (interactionState != InteractionState.CONNECTING_ELEMENTS) {
                return;
            }
            Point2D point = sceneToLocal(event.getSceneX(), event.getSceneY());
            connectionPreview.setEndX(point.getX());
            connectionPreview.setEndY(point.getY());
            updateConnectionTarget(event.getPickResult().getIntersectedNode());
            event.consume();
        });
        handle.setOnMouseReleased(event -> {
            if (interactionState != InteractionState.CONNECTING_ELEMENTS) {
                return;
            }
            BoardElementView target = findElementView(event.getPickResult().getIntersectedNode());
            if (target != null && connectionService.canConnect(connectionSourceId, target.elementId())) {
                Connection[] created = new Connection[1];
                commandHistory.execute("接続追加", () -> created[0] =
                        connectionService.createConnection(connectionSourceId, target.elementId()));
                addConnectionView(created[0]);
                selectConnection(created[0].id());
                commitStatus("接続を作成しました");
            } else {
                statusListener.accept("接続をキャンセルしました");
            }
            endConnecting();
            event.consume();
        });
    }

    private void installResizeHandlers(BoxElementView view) {
        view.resizeHandle().setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY
                    || interactionState != InteractionState.IDLE
                    || spacePressed) {
                return;
            }
            selectOnly(view.elementId());
            gestureStart = toWorld(event);
            resizeStartSize = requireElement(view.elementId()).size();
            interactionStartSnapshot = document.snapshot();
            interactionState = InteractionState.RESIZING_ELEMENT;
            statusListener.accept("ボックスをリサイズ中");
            event.consume();
        });
        view.resizeHandle().setOnMouseDragged(event -> {
            if (interactionState != InteractionState.RESIZING_ELEMENT) {
                return;
            }
            Point2D current = toWorld(event);
            double width = Math.max(BoardConfiguration.MINIMUM_BOX_WIDTH,
                    resizeStartSize.width() + current.getX() - gestureStart.getX());
            double height = Math.max(BoardConfiguration.MINIMUM_BOX_HEIGHT,
                    resizeStartSize.height() + current.getY() - gestureStart.getY());
            BoardElement updated = boardService.resizeElement(
                    view.elementId(), new ElementSize(width, height));
            view.update(updated);
            updateRelatedConnections(view.elementId());
            notifySelection();
            event.consume();
        });
        view.resizeHandle().setOnMouseReleased(event -> {
            if (interactionState == InteractionState.RESIZING_ELEMENT) {
                commandHistory.recordAppliedChange("ボックスサイズ変更", interactionStartSnapshot);
                finishInteraction("ボックスをリサイズしました", true);
                event.consume();
            }
        });
    }

    private void addConnectionView(Connection connection) {
        ConnectionView view = new ConnectionView(connection);
        connectionViews.put(connection.id(), view);
        connectionLayer.getChildren().add(view);
        view.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectConnection(connection.id());
                requestFocus();
                event.consume();
            }
        });
        updateConnection(view);
    }

    private void updateConnection(ConnectionView view) {
        Connection connection = view.connection();
        BoardElement source = requireElement(connection.sourceElementId());
        BoardElement target = requireElement(connection.targetElementId());
        Point2D sourceCenter = centerOf(source);
        Point2D targetCenter = centerOf(target);
        Point2D sourceEdge = edgePoint(source, targetCenter);
        Point2D targetEdge = edgePoint(target, sourceCenter);
        view.update(sourceEdge.getX(), sourceEdge.getY(), targetEdge.getX(), targetEdge.getY());
    }

    private void updateRelatedConnections(UUID elementId) {
        for (Connection connection : connectionService.findRelatedConnections(elementId)) {
            ConnectionView view = connectionViews.get(connection.id());
            if (view != null) {
                updateConnection(view);
            }
        }
    }

    private void updateSelectionRectangle(double x, double y) {
        selectionRectangle.setX(Math.min(gestureStart.getX(), x));
        selectionRectangle.setY(Math.min(gestureStart.getY(), y));
        selectionRectangle.setWidth(Math.abs(x - gestureStart.getX()));
        selectionRectangle.setHeight(Math.abs(y - gestureStart.getY()));
    }

    private void completeRangeSelection(boolean additive) {
        Point2D first = viewportTransform.screenToWorld(
                selectionRectangle.getX(), selectionRectangle.getY());
        Point2D second = viewportTransform.screenToWorld(
                selectionRectangle.getX() + selectionRectangle.getWidth(),
                selectionRectangle.getY() + selectionRectangle.getHeight());
        if (!additive) {
            selectedElementIds.clear();
        }
        selectedConnectionId = null;
        for (BoardElement element : document.elements()) {
            double right = element.position().x() + element.size().width();
            double bottom = element.position().y() + element.size().height();
            if (element.position().x() >= first.getX()
                    && element.position().y() >= first.getY()
                    && right <= second.getX()
                    && bottom <= second.getY()) {
                selectedElementIds.add(element.id());
            }
        }
        selectionRectangle.setVisible(false);
        applySelectionStyles();
        notifySelection();
        statusListener.accept(selectedElementIds.isEmpty()
                ? "選択ツール"
                : selectedElementIds.size() + "個の要素を選択中");
    }

    private void selectOnly(UUID elementId) {
        selectedConnectionId = null;
        selectedElementIds.clear();
        selectedElementIds.add(elementId);
        applySelectionStyles();
        notifySelection();
    }

    private void selectElements(Set<UUID> elementIds) {
        selectedConnectionId = null;
        selectedElementIds.clear();
        selectedElementIds.addAll(elementIds);
        applySelectionStyles();
        notifySelection();
    }

    private void toggleElementSelection(UUID elementId) {
        selectedConnectionId = null;
        if (!selectedElementIds.remove(elementId)) {
            selectedElementIds.add(elementId);
        }
        applySelectionStyles();
        notifySelection();
    }

    private void selectConnection(UUID connectionId) {
        clearSelectionState();
        selectedConnectionId = connectionId;
        applySelectionStyles();
        notifySelection();
    }

    private void clearSelectionState() {
        selectedElementIds.clear();
        selectedConnectionId = null;
    }

    private void applySelectionStyles() {
        for (Map.Entry<UUID, BoardElementView> entry : elementViews.entrySet()) {
            entry.getValue().setSelected(selectedElementIds.contains(entry.getKey()));
        }
        for (Map.Entry<UUID, ConnectionView> entry : connectionViews.entrySet()) {
            entry.getValue().setSelected(Objects.equals(selectedConnectionId, entry.getKey()));
        }
    }

    private void notifySelection() {
        Connection connection = selectedConnectionId == null
                ? null
                : document.findConnection(selectedConnectionId).orElse(null);
        selectionListener.accept(new SelectionInfo(selectedElements(), connection));
    }

    private List<BoardElement> selectedElements() {
        return selectedElementIds.stream()
                .map(document::findElement)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private void cancelTemporaryOperation() {
        if (interactionState == InteractionState.CONNECTING_ELEMENTS) {
            endConnecting();
        } else if (interactionStartSnapshot != null
                && (interactionState == InteractionState.DRAGGING_ELEMENT
                || interactionState == InteractionState.RESIZING_ELEMENT)) {
            document.restore(interactionStartSnapshot);
            rebuildViews();
            applySelectionStyles();
        }
        selectionRectangle.setVisible(false);
        interactionStartSnapshot = null;
        interactionState = InteractionState.IDLE;
        notifySelection();
        statusListener.accept("操作をキャンセルしました");
    }

    private void endConnecting() {
        if (connectionTargetId != null && elementViews.containsKey(connectionTargetId)) {
            elementViews.get(connectionTargetId).setConnectionTarget(false);
        }
        connectionPreview.setVisible(false);
        connectionSourceId = null;
        connectionTargetId = null;
        interactionState = InteractionState.IDLE;
    }

    private void updateConnectionTarget(Node pickedNode) {
        BoardElementView candidate = findElementView(pickedNode);
        UUID nextTargetId = candidate != null
                && connectionService.canConnect(connectionSourceId, candidate.elementId())
                ? candidate.elementId()
                : null;
        if (Objects.equals(connectionTargetId, nextTargetId)) {
            return;
        }
        if (connectionTargetId != null) {
            elementViews.get(connectionTargetId).setConnectionTarget(false);
        }
        connectionTargetId = nextTargetId;
        if (connectionTargetId != null) {
            elementViews.get(connectionTargetId).setConnectionTarget(true);
        }
    }

    private void restoreAfterHistory(String status) {
        clearSelectionState();
        viewportTransform.apply(document.viewSettings());
        rebuildViews();
        notifySelection();
        notifyViewportChanged(false);
        commitStatus(status);
    }

    private void finishInteraction(String status, boolean changed) {
        interactionStartSnapshot = null;
        interactionState = InteractionState.IDLE;
        if (changed) {
            commitStatus(status);
        } else {
            statusListener.accept(status);
        }
    }

    private void commitStatus(String status) {
        statusListener.accept(status);
        changeListener.run();
    }

    private void notifyViewportChanged(boolean markChanged) {
        document.setViewSettings(viewportTransform.settings());
        drawGrid();
        zoomListener.accept(viewportTransform.zoom());
        if (markChanged) {
            changeListener.run();
        }
    }

    private void rebuildViews() {
        elementLayer.getChildren().clear();
        connectionLayer.getChildren().clear();
        elementViews.clear();
        connectionViews.clear();
        for (BoardElement element : document.elements()) {
            addElementView(element);
        }
        for (Connection connection : document.connections()) {
            addConnectionView(connection);
        }
        emptyHint.setVisible(document.elements().isEmpty());
        applySelectionStyles();
    }

    private void focusElements(List<BoardElement> elements) {
        if (elements.isEmpty()) {
            viewportTransform.set(1.0, getWidth() / 2.0, getHeight() / 2.0);
            notifyViewportChanged(true);
            statusListener.accept("表示をリセットしました");
            return;
        }
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (BoardElement element : elements) {
            minX = Math.min(minX, element.position().x());
            minY = Math.min(minY, element.position().y());
            maxX = Math.max(maxX, element.position().x() + element.size().width());
            maxY = Math.max(maxY, element.position().y() + element.size().height());
        }
        double contentWidth = Math.max(1.0, maxX - minX);
        double contentHeight = Math.max(1.0, maxY - minY);
        double availableWidth = Math.max(1.0, getWidth() - VIEW_MARGIN * 2.0);
        double availableHeight = Math.max(1.0, getHeight() - VIEW_MARGIN * 2.0);
        double zoom = Math.clamp(
                Math.min(availableWidth / contentWidth, availableHeight / contentHeight),
                BoardConfiguration.MINIMUM_ZOOM,
                BoardConfiguration.MAXIMUM_ZOOM);
        double offsetX = getWidth() / 2.0 - (minX + maxX) / 2.0 * zoom;
        double offsetY = getHeight() / 2.0 - (minY + maxY) / 2.0 * zoom;
        viewportTransform.set(zoom, offsetX, offsetY);
        notifyViewportChanged(true);
        statusListener.accept("表示範囲を調整しました");
    }

    private void drawGrid() {
        GraphicsContext graphics = backgroundLayer.getGraphicsContext2D();
        graphics.clearRect(0.0, 0.0, backgroundLayer.getWidth(), backgroundLayer.getHeight());
        double spacing = BoardConfiguration.GRID_SPACING * viewportTransform.zoom();
        double startX = positiveModulo(viewportTransform.offsetX(), spacing);
        double startY = positiveModulo(viewportTransform.offsetY(), spacing);
        graphics.setFill(GRID_COLOR);
        for (double x = startX; x < backgroundLayer.getWidth(); x += spacing) {
            for (double y = startY; y < backgroundLayer.getHeight(); y += spacing) {
                graphics.fillOval(x - 1.0, y - 1.0, 2.0, 2.0);
            }
        }
    }

    private Point2D toWorld(MouseEvent event) {
        Point2D local = sceneToLocal(event.getSceneX(), event.getSceneY());
        return viewportTransform.screenToWorld(local.getX(), local.getY());
    }

    private Position centeredPosition(Point2D center, ElementSize size) {
        return new Position(center.getX() - size.width() / 2.0,
                center.getY() - size.height() / 2.0);
    }

    private Point2D connectionHandleScreenPoint(UUID elementId) {
        BoardElement element = requireElement(elementId);
        return viewportTransform.worldToScreen(
                element.position().x() + element.size().width(),
                element.position().y() + element.size().height() / 2.0);
    }

    private Point2D centerOf(BoardElement element) {
        return new Point2D(
                element.position().x() + element.size().width() / 2.0,
                element.position().y() + element.size().height() / 2.0);
    }

    private Point2D edgePoint(BoardElement element, Point2D toward) {
        Point2D center = centerOf(element);
        double deltaX = toward.getX() - center.getX();
        double deltaY = toward.getY() - center.getY();
        if (deltaX == 0.0 && deltaY == 0.0) {
            return center;
        }
        double scaleX = element.size().width() / 2.0 / Math.max(Math.abs(deltaX), 0.0001);
        double scaleY = element.size().height() / 2.0 / Math.max(Math.abs(deltaY), 0.0001);
        double scale = Math.min(scaleX, scaleY);
        return new Point2D(center.getX() + deltaX * scale, center.getY() + deltaY * scale);
    }

    private BoardElementView findElementView(Node node) {
        Node current = node;
        while (current != null && current != this) {
            if (current instanceof BoardElementView view) {
                return view;
            }
            current = current.getParent();
        }
        return null;
    }

    private ConnectionView findConnectionView(Node node) {
        Node current = node;
        while (current != null && current != this) {
            if (current instanceof ConnectionView view) {
                return view;
            }
            current = current.getParent();
        }
        return null;
    }

    private boolean isElementOrConnection(Object target) {
        return target instanceof Node node
                && (findElementView(node) != null || findConnectionView(node) != null);
    }

    private BoardElement requireElement(UUID elementId) {
        return document.findElement(elementId)
                .orElseThrow(() -> new IllegalStateException("表示対象の要素が存在しません。"));
    }

    private static double positiveModulo(double value, double divisor) {
        return ((value % divisor) + divisor) % divisor;
    }

    private void ensureDocument() {
        if (document == null) {
            throw new IllegalStateException("BoardDocumentが設定されていません。");
        }
    }
}
