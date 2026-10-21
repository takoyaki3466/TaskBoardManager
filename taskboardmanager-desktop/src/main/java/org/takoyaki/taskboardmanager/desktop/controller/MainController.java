package org.takoyaki.taskboardmanager.desktop.controller;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.takoyaki.taskboardmanager.core.NodeSearchCriteria;
import org.takoyaki.taskboardmanager.core.NodeSearchService;
import org.takoyaki.taskboardmanager.core.TaskProgressService;
import org.takoyaki.taskboardmanager.desktop.view.BoardCanvas;
import org.takoyaki.taskboardmanager.desktop.view.HierarchyView;
import org.takoyaki.taskboardmanager.desktop.view.OutlineView;
import org.takoyaki.taskboardmanager.desktop.view.SelectionInfo;
import org.takoyaki.taskboardmanager.desktop.view.ViewMode;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardElement;
import org.takoyaki.taskboardmanager.model.BoxElement;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.DecisionProperties;
import org.takoyaki.taskboardmanager.model.IdeaProperties;
import org.takoyaki.taskboardmanager.model.IdeaStatus;
import org.takoyaki.taskboardmanager.model.NodeElement;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.Priority;
import org.takoyaki.taskboardmanager.model.ProgressMode;
import org.takoyaki.taskboardmanager.model.QuestionProperties;
import org.takoyaki.taskboardmanager.model.QuestionStatus;
import org.takoyaki.taskboardmanager.model.RelationType;
import org.takoyaki.taskboardmanager.model.TaskProperties;
import org.takoyaki.taskboardmanager.model.TaskStatus;

/** ファイルセッション、3表示、検索、インスペクターを連携します。 */
public final class MainController {

    @FXML private BoardCanvas boardCanvas;
    @FXML private HierarchyView hierarchyView;
    @FXML private OutlineView outlineView;
    @FXML private ToggleButton freeboardButton;
    @FXML private ToggleButton hierarchyButton;
    @FXML private ToggleButton outlineButton;
    @FXML private TextField boardTitleField;
    @FXML private TextField elementTitleField;
    @FXML private TextField xField;
    @FXML private TextField yField;
    @FXML private TextField widthField;
    @FXML private TextField heightField;
    @FXML private TextField searchField;
    @FXML private Label selectionTypeLabel;
    @FXML private Label elementIdLabel;
    @FXML private Label connectionLabel;
    @FXML private Label documentStatsLabel;
    @FXML private Label statusLabel;
    @FXML private Label zoomLabel;
    @FXML private Label selectionCountLabel;
    @FXML private Label modifiedLabel;
    @FXML private Label currentFileLabel;
    @FXML private MenuItem newMenuItem;
    @FXML private MenuItem openMenuItem;
    @FXML private MenuItem saveMenuItem;
    @FXML private MenuItem saveAsMenuItem;
    @FXML private ComboBox<NodeType> nodeTypeCombo;
    @FXML private TextArea descriptionArea;
    @FXML private VBox nodeDetailsPane;
    @FXML private VBox taskPane;
    @FXML private VBox questionPane;
    @FXML private VBox ideaPane;
    @FXML private VBox decisionPane;
    @FXML private ComboBox<TaskStatus> taskStatusCombo;
    @FXML private ComboBox<Priority> priorityCombo;
    @FXML private ComboBox<ProgressMode> progressModeCombo;
    @FXML private TextField progressField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker dueDatePicker;
    @FXML private ComboBox<QuestionStatus> questionStatusCombo;
    @FXML private TextArea resolutionArea;
    @FXML private ComboBox<IdeaStatus> ideaStatusCombo;
    @FXML private TextArea decisionArea;
    @FXML private TextArea rationaleArea;
    @FXML private DatePicker decidedDatePicker;
    @FXML private VBox connectionPane;
    @FXML private ComboBox<RelationType> relationTypeCombo;
    @FXML private ComboBox<NodeType> filterNodeType;
    @FXML private ComboBox<TaskStatus> filterTaskStatus;
    @FXML private ComboBox<Priority> filterPriority;
    @FXML private ComboBox<QuestionStatus> filterQuestionStatus;
    @FXML private ListView<NodeElement> searchResultsList;

    private final DocumentFileManager fileManager = new DocumentFileManager();
    private final NodeSearchService searchService = new NodeSearchService();
    private final TaskProgressService progressService = new TaskProgressService();
    private SelectionInfo selection = SelectionInfo.empty();
    private ViewMode viewMode = ViewMode.FREEBOARD;

    @FXML
    private void initialize() {
        configureControls();
        boardCanvas.setSelectionListener(this::showSelection);
        boardCanvas.setEditRequestHandler(this::showTitleDialog);
        boardCanvas.setStatusListener(statusLabel::setText);
        boardCanvas.setZoomListener(zoom -> zoomLabel.setText(String.format("%.0f%%", zoom * 100.0)));
        boardCanvas.setChangeListener(this::handleDocumentChanged);
        hierarchyView.setSelectionListener(this::selectFromAlternativeView);
        outlineView.setSelectionListener(this::selectFromAlternativeView);
        configureFileShortcuts();
        installDocument(new BoardDocument(UUID.randomUUID(), "無題のボード"), null);
    }

    public boolean requestClose() {
        return confirmCanDiscardOrSave();
    }

    public void installSceneShortcuts(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl) {
                return;
            }
            if (event.getCode() == KeyCode.DELETE) {
                handleDelete();
                event.consume();
                return;
            }
            if (!event.isShortcutDown()) {
                return;
            }
            if (event.getCode() == KeyCode.Z && event.isShiftDown()) {
                handleRedo();
            } else if (event.getCode() == KeyCode.Z) {
                handleUndo();
            } else if (event.getCode() == KeyCode.Y) {
                handleRedo();
            } else if (event.getCode() == KeyCode.C) {
                handleCopy();
            } else if (event.getCode() == KeyCode.V) {
                handlePaste();
            } else if (event.getCode() == KeyCode.X) {
                handleCut();
            } else if (event.getCode() == KeyCode.A) {
                handleSelectAll();
            } else {
                return;
            }
            event.consume();
        });
    }

    @FXML
    private void handleNew() {
        if (confirmCanDiscardOrSave()) {
            installDocument(new BoardDocument(UUID.randomUUID(), "無題のボード"), null);
        }
    }

    @FXML
    private void handleOpen() {
        if (confirmCanDiscardOrSave()) {
            fileManager.chooseAndLoad(owner()).ifPresent(
                    loaded -> installDocument(loaded.document(), loaded.path()));
        }
    }

    @FXML
    private void handleSave() {
        saveDocument(false);
    }

    @FXML
    private void handleSaveAs() {
        saveDocument(true);
    }

    @FXML
    private void handleAddNode() {
        switchView(ViewMode.FREEBOARD);
        boardCanvas.addNodeAtCenter();
    }

    @FXML
    private void handleAddBox() {
        switchView(ViewMode.FREEBOARD);
        boardCanvas.addBoxAtCenter();
    }

    @FXML
    private void handleConnectionHint() {
        switchView(ViewMode.FREEBOARD);
        boardCanvas.showConnectionHint();
    }

    @FXML
    private void handleUndo() {
        boardCanvas.undo();
        handleDocumentChanged();
    }

    @FXML
    private void handleRedo() {
        boardCanvas.redo();
        handleDocumentChanged();
    }

    @FXML
    private void handleCopy() {
        boardCanvas.copySelection();
    }

    @FXML
    private void handlePaste() {
        boardCanvas.paste();
    }

    @FXML
    private void handleCut() {
        boardCanvas.cutSelection();
    }

    @FXML
    private void handleSelectAll() {
        boardCanvas.selectAllElements();
    }

    @FXML
    private void handleDelete() {
        boardCanvas.deleteSelection();
    }

    @FXML
    private void handleResetZoom() {
        boardCanvas.resetZoom();
    }

    @FXML
    private void handleShowAll() {
        boardCanvas.showAllElements();
    }

    @FXML
    private void handleFocusSelection() {
        boardCanvas.focusSelection();
    }

    @FXML
    private void handleFreeboardMode() {
        switchView(ViewMode.FREEBOARD);
    }

    @FXML
    private void handleHierarchyMode() {
        switchView(ViewMode.HIERARCHY);
    }

    @FXML
    private void handleOutlineMode() {
        switchView(ViewMode.OUTLINE);
    }

    @FXML
    private void handleApplyLayout() {
        boardCanvas.applyHierarchicalLayout();
        switchView(ViewMode.FREEBOARD);
    }

    @FXML
    private void handleBoardRename() {
        try {
            boardCanvas.renameBoard(boardTitleField.getText());
        } catch (IllegalArgumentException exception) {
            boardTitleField.setText(boardCanvas.document().title());
            statusLabel.setText(exception.getMessage());
        }
    }

    @FXML
    private void handleApplyProperties() {
        try {
            if (selection.connection() != null && selection.elements().isEmpty()) {
                boardCanvas.updateSelectedConnectionRelation(requireValue(relationTypeCombo, "接続関係"));
                return;
            }
            if (selection.elements().size() != 1) {
                return;
            }
            BoardElement element = selection.elements().getFirst();
            double x = parseFinite(xField, "X座標");
            double y = parseFinite(yField, "Y座標");
            if (element instanceof NodeElement node) {
                NodeType type = requireValue(nodeTypeCombo, "ノード種類");
                TaskProperties task = type == NodeType.TASK ? taskProperties(node) : null;
                QuestionProperties question = type == NodeType.QUESTION
                        ? new QuestionProperties(
                                requireValue(questionStatusCombo, "疑問状態"),
                                resolutionArea.getText())
                        : null;
                IdeaProperties idea = type == NodeType.IDEA
                        ? new IdeaProperties(requireValue(ideaStatusCombo, "アイデア状態")) : null;
                DecisionProperties decision = type == NodeType.DECISION
                        ? new DecisionProperties(decisionArea.getText(), rationaleArea.getText(),
                                decisionTime(node)) : null;
                boardCanvas.updateSelectedNode(elementTitleField.getText(), x, y, type,
                        descriptionArea.getText(), task, question, idea, decision);
            } else {
                boardCanvas.updateSelectedElement(elementTitleField.getText(), x, y,
                        parseFinite(widthField, "幅"), parseFinite(heightField, "高さ"));
            }
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
            showSelection(selection);
        }
    }

    @FXML
    private void handleSearch() {
        List<NodeElement> results = searchService.search(boardCanvas.document(), new NodeSearchCriteria(
                searchField.getText(), filterNodeType.getValue(), filterTaskStatus.getValue(),
                filterPriority.getValue(), filterQuestionStatus.getValue()));
        searchResultsList.setItems(FXCollections.observableArrayList(results));
        statusLabel.setText(results.size() + "件のノードが見つかりました");
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        filterNodeType.setValue(null);
        filterTaskStatus.setValue(null);
        filterPriority.setValue(null);
        filterQuestionStatus.setValue(null);
        handleSearch();
    }

    private void configureControls() {
        ToggleGroup modes = new ToggleGroup();
        freeboardButton.setToggleGroup(modes);
        hierarchyButton.setToggleGroup(modes);
        outlineButton.setToggleGroup(modes);
        nodeTypeCombo.getItems().setAll(NodeType.values());
        taskStatusCombo.getItems().setAll(TaskStatus.values());
        priorityCombo.getItems().setAll(Priority.values());
        progressModeCombo.getItems().setAll(ProgressMode.values());
        questionStatusCombo.getItems().setAll(QuestionStatus.values());
        ideaStatusCombo.getItems().setAll(IdeaStatus.values());
        relationTypeCombo.getItems().setAll(RelationType.values());
        filterNodeType.getItems().setAll(NodeType.values());
        filterTaskStatus.getItems().setAll(TaskStatus.values());
        filterPriority.getItems().setAll(Priority.values());
        filterQuestionStatus.getItems().setAll(QuestionStatus.values());
        resetDetailFields();
        nodeTypeCombo.valueProperty().addListener(
                (observable, oldValue, newValue) -> showTypeSpecificPanes(newValue));
        progressModeCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                progressField.setDisable(newValue == ProgressMode.CHILD_AVERAGE));
        searchResultsList.setCellFactory(ignored -> new ListCell<>() {
            @Override protected void updateItem(NodeElement item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.nodeType().icon() + " " + item.title());
            }
        });
        searchResultsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, node) -> {
            if (node != null) {
                navigateTo(node.id());
            }
        });
    }

    private TaskProperties taskProperties(NodeElement node) {
        ProgressMode mode = requireValue(progressModeCombo, "進捗モード");
        int progress = mode == ProgressMode.CHILD_AVERAGE
                ? progressService.childAverage(boardCanvas.document(), node.id())
                : parseProgress();
        return new TaskProperties(requireValue(taskStatusCombo, "タスク状態"), progress,
                requireValue(priorityCombo, "優先度"), mode,
                startDatePicker.getValue(), dueDatePicker.getValue());
    }

    private LocalDateTime decisionTime(NodeElement node) {
        if (decidedDatePicker.getValue() == null) {
            return null;
        }
        LocalDateTime current = node.decisionProperties() == null
                ? null
                : node.decisionProperties().decidedAt();
        if (current != null && current.toLocalDate().equals(decidedDatePicker.getValue())) {
            return current;
        }
        return decidedDatePicker.getValue().atStartOfDay();
    }

    private void installDocument(BoardDocument document, Path path) {
        boardCanvas.setDocument(document);
        hierarchyView.setDocument(document);
        outlineView.setDocument(document);
        fileManager.install(document, path);
        showSelection(SelectionInfo.empty());
        switchView(ViewMode.FREEBOARD);
        handleDocumentChanged();
    }

    private void showSelection(SelectionInfo selection) {
        this.selection = selection;
        List<BoardElement> elements = selection.elements();
        boolean single = elements.size() == 1;
        setElementFieldsDisabled(!single);
        showPane(nodeDetailsPane, false);
        showPane(connectionPane, false);
        connectionLabel.setText("—");
        if (single) {
            BoardElement element = elements.getFirst();
            selectionTypeLabel.setText(element instanceof BoxElement ? "ボックス" : "ノード");
            elementIdLabel.setText(element.id().toString());
            elementTitleField.setText(element.title());
            xField.setText(formatNumber(element.position().x()));
            yField.setText(formatNumber(element.position().y()));
            widthField.setText(formatNumber(element.size().width()));
            heightField.setText(formatNumber(element.size().height()));
            widthField.setDisable(!(element instanceof BoxElement));
            heightField.setDisable(!(element instanceof BoxElement));
            if (element instanceof NodeElement node) {
                populateNode(node);
                showPane(nodeDetailsPane, true);
                hierarchyView.selectNode(node.id());
                outlineView.selectNode(node.id());
            }
            long count = boardCanvas.document().connections().stream()
                    .filter(connection -> connection.sourceElementId().equals(element.id())
                            || connection.targetElementId().equals(element.id()))
                    .count();
            connectionLabel.setText(count + "件");
        } else if (elements.size() > 1) {
            selectionTypeLabel.setText("複数選択（" + elements.size() + "件）");
            clearElementFields();
        } else if (selection.connection() != null) {
            Connection connection = selection.connection();
            selectionTypeLabel.setText("接続");
            elementIdLabel.setText(connection.id().toString());
            connectionLabel.setText(elementTitle(connection.sourceElementId())
                    + " → " + elementTitle(connection.targetElementId()));
            relationTypeCombo.setValue(connection.relationType());
            clearElementFields();
            showPane(connectionPane, true);
        } else {
            selectionTypeLabel.setText("未選択");
            elementIdLabel.setText("—");
            clearElementFields();
        }
        selectionCountLabel.setText("選択: " + elements.size());
        updateDocumentStatus();
    }

    private void populateNode(NodeElement node) {
        resetDetailFields();
        nodeTypeCombo.setValue(node.nodeType());
        descriptionArea.setText(node.description());
        if (node.taskProperties() != null) {
            TaskProperties task = node.taskProperties();
            taskStatusCombo.setValue(task.status());
            priorityCombo.setValue(task.priority());
            progressModeCombo.setValue(task.progressMode());
            progressField.setText(Integer.toString(task.progress()));
            startDatePicker.setValue(task.startDate());
            dueDatePicker.setValue(task.dueDate());
        }
        if (node.questionProperties() != null) {
            questionStatusCombo.setValue(node.questionProperties().status());
            resolutionArea.setText(node.questionProperties().resolution());
        }
        if (node.ideaProperties() != null) {
            ideaStatusCombo.setValue(node.ideaProperties().status());
        }
        if (node.decisionProperties() != null) {
            decisionArea.setText(node.decisionProperties().decision());
            rationaleArea.setText(node.decisionProperties().rationale());
            LocalDateTime decidedAt = node.decisionProperties().decidedAt();
            decidedDatePicker.setValue(decidedAt == null ? null : decidedAt.toLocalDate());
        }
        showTypeSpecificPanes(node.nodeType());
    }

    private void resetDetailFields() {
        nodeTypeCombo.setValue(NodeType.GENERAL);
        descriptionArea.clear();
        taskStatusCombo.setValue(TaskProperties.DEFAULT.status());
        priorityCombo.setValue(TaskProperties.DEFAULT.priority());
        progressModeCombo.setValue(TaskProperties.DEFAULT.progressMode());
        progressField.setText(Integer.toString(TaskProperties.DEFAULT.progress()));
        startDatePicker.setValue(null);
        dueDatePicker.setValue(null);
        questionStatusCombo.setValue(QuestionProperties.DEFAULT.status());
        resolutionArea.clear();
        ideaStatusCombo.setValue(IdeaProperties.DEFAULT.status());
        decisionArea.clear();
        rationaleArea.clear();
        decidedDatePicker.setValue(null);
    }

    private void switchView(ViewMode mode) {
        viewMode = mode;
        setViewVisible(boardCanvas, mode == ViewMode.FREEBOARD);
        setViewVisible(hierarchyView, mode == ViewMode.HIERARCHY);
        setViewVisible(outlineView, mode == ViewMode.OUTLINE);
        freeboardButton.setSelected(mode == ViewMode.FREEBOARD);
        hierarchyButton.setSelected(mode == ViewMode.HIERARCHY);
        outlineButton.setSelected(mode == ViewMode.OUTLINE);
        if (mode == ViewMode.HIERARCHY) {
            hierarchyView.refresh();
        }
        if (mode == ViewMode.OUTLINE) {
            outlineView.refresh();
        }
    }

    private void navigateTo(UUID id) {
        boardCanvas.selectElement(id);
        if (viewMode == ViewMode.FREEBOARD) {
            boardCanvas.selectAndFocus(id);
        } else if (viewMode == ViewMode.HIERARCHY) {
            hierarchyView.selectNode(id);
        } else {
            outlineView.selectNode(id);
        }
    }

    private void selectFromAlternativeView(UUID id) {
        boardCanvas.selectElement(id);
    }

    private void handleDocumentChanged() {
        updateDocumentStatus();
        hierarchyView.refresh();
        outlineView.refresh();
        handleSearchSilently();
    }

    private void handleSearchSilently() {
        searchResultsList.setItems(FXCollections.observableArrayList(searchService.search(
                boardCanvas.document(), new NodeSearchCriteria(searchField.getText(), filterNodeType.getValue(),
                        filterTaskStatus.getValue(), filterPriority.getValue(), filterQuestionStatus.getValue()))));
    }

    private void showTypeSpecificPanes(NodeType type) {
        showPane(taskPane, type == NodeType.TASK);
        showPane(questionPane, type == NodeType.QUESTION);
        showPane(ideaPane, type == NodeType.IDEA);
        showPane(decisionPane, type == NodeType.DECISION);
    }

    private void showTitleDialog(BoardElement element) {
        TextInputDialog dialog = new TextInputDialog(element.title());
        dialog.initOwner(owner());
        dialog.setTitle("タイトルを編集");
        dialog.setHeaderText(null);
        dialog.setContentText("タイトル");
        dialog.showAndWait().ifPresent(title -> {
            elementTitleField.setText(title);
            handleApplyProperties();
        });
    }

    private boolean saveDocument(boolean choosePath) {
        if (fileManager.save(boardCanvas.document(), owner(), choosePath)) {
            statusLabel.setText("保存しました");
            updateDocumentStatus();
            return true;
        }
        return false;
    }

    private boolean confirmCanDiscardOrSave() {
        return fileManager.confirmCanProceed(boardCanvas.document(), owner());
    }

    private void updateDocumentStatus() {
        BoardDocument document = boardCanvas.document();
        boardTitleField.setText(document.title());
        documentStatsLabel.setText(document.elements().size() + "要素 / "
                + document.connections().size() + "接続 / rev " + document.revision());
        modifiedLabel.setText(fileManager.isModified(document) ? "変更あり" : "保存済み");
        currentFileLabel.setText(fileManager.displayedFileName());
    }

    private void configureFileShortcuts() {
        newMenuItem.setAccelerator(shortcut(KeyCode.N, false));
        openMenuItem.setAccelerator(shortcut(KeyCode.O, false));
        saveMenuItem.setAccelerator(shortcut(KeyCode.S, false));
        saveAsMenuItem.setAccelerator(shortcut(KeyCode.S, true));
    }

    private KeyCombination shortcut(KeyCode code, boolean shift) {
        return shift ? new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
                : new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN);
    }

    private String elementTitle(UUID id) {
        return boardCanvas.document().findElement(id)
                .map(BoardElement::title)
                .orElse(id.toString());
    }

    private int parseProgress() {
        try {
            return Integer.parseInt(progressField.getText().trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "進捗率には0から100の整数を入力してください。", exception);
        }
    }

    private double parseFinite(TextField field, String name) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            if (!Double.isFinite(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    name + "には有限の数値を入力してください。", exception);
        }
    }

    private <T> T requireValue(ComboBox<T> comboBox, String name) {
        if (comboBox.getValue() == null) {
            throw new IllegalArgumentException(name + "を選択してください。");
        }
        return comboBox.getValue();
    }

    private void setElementFieldsDisabled(boolean disabled) {
        elementTitleField.setDisable(disabled);
        xField.setDisable(disabled);
        yField.setDisable(disabled);
        widthField.setDisable(disabled);
        heightField.setDisable(disabled);
    }

    private void clearElementFields() {
        elementTitleField.clear();
        xField.clear();
        yField.clear();
        widthField.clear();
        heightField.clear();
    }

    private void showPane(VBox pane, boolean visible) {
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void setViewVisible(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private String formatNumber(double value) {
        return String.format("%.1f", value);
    }

    private Window owner() {
        return boardCanvas.getScene() == null ? null : boardCanvas.getScene().getWindow();
    }
}
