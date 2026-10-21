package org.takoyaki.taskboardmanager.desktop.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.takoyaki.taskboardmanager.desktop.controller.DialogService.UnsavedChoice;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardSnapshot;
import org.takoyaki.taskboardmanager.persistence.JsonBoardRepository;

/** ファイル選択、保存チェックポイント、未保存確認を管理します。 */
public final class DocumentFileManager {

    private final JsonBoardRepository repository = new JsonBoardRepository();
    private final DialogService dialogs = new DialogService();
    private BoardSnapshot savedSnapshot;
    private Path currentPath;

    public void install(BoardDocument document, Path path) {
        currentPath = path;
        savedSnapshot = document.snapshot();
    }

    public boolean isModified(BoardDocument document) {
        return savedSnapshot != null && !savedSnapshot.equals(document.snapshot());
    }

    public String displayedFileName() {
        return currentPath == null ? "未保存" : currentPath.getFileName().toString();
    }

    /** 選択されたファイルを完全に読み込み、成功した場合だけ結果を返します。 */
    public Optional<LoadedDocument> chooseAndLoad(Window owner) {
        FileChooser chooser = createFileChooser("ボードを開く");
        java.io.File selectedFile = chooser.showOpenDialog(owner);
        if (selectedFile == null) {
            return Optional.empty();
        }
        try {
            Path path = selectedFile.toPath().toAbsolutePath().normalize();
            return Optional.of(new LoadedDocument(repository.load(path), path));
        } catch (IOException exception) {
            dialogs.showError(owner, "ファイルを開けません", exception.getMessage());
            return Optional.empty();
        }
    }

    /** 保存先選択を含めて保存し、成功時にチェックポイントを更新します。 */
    public boolean save(BoardDocument document, Window owner, boolean choosePath) {
        Path destination = currentPath;
        if (choosePath || destination == null) {
            FileChooser chooser = createFileChooser("名前を付けて保存");
            java.io.File selectedFile = chooser.showSaveDialog(owner);
            if (selectedFile == null) {
                return false;
            }
            destination = ensureFileSuffix(selectedFile.toPath());
        }
        try {
            repository.save(document, destination);
            currentPath = destination.toAbsolutePath().normalize();
            savedSnapshot = document.snapshot();
            return true;
        } catch (IOException exception) {
            dialogs.showError(owner, "保存できません", exception.getMessage());
            return false;
        }
    }

    public boolean confirmCanProceed(BoardDocument document, Window owner) {
        if (!isModified(document)) {
            return true;
        }
        UnsavedChoice choice = dialogs.confirmUnsaved(owner);
        return switch (choice) {
            case SAVE -> save(document, owner, false);
            case DISCARD -> true;
            case CANCEL -> false;
        };
    }

    private FileChooser createFileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "TaskBoardManager (*.tbm.json)", "*.tbm.json"),
                new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
        return chooser;
    }

    private Path ensureFileSuffix(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(JsonBoardRepository.FILE_SUFFIX)) {
            return path;
        }
        return path.resolveSibling(fileName + JsonBoardRepository.FILE_SUFFIX);
    }

    /** 読み込み済みドキュメントと元ファイルをまとめます。 */
    public record LoadedDocument(BoardDocument document, Path path) {
    }
}
