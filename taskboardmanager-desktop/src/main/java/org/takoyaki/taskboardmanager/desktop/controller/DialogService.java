package org.takoyaki.taskboardmanager.desktop.controller;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** 確認とエラー通知を一貫したJavaFXダイアログとして提供します。 */
public final class DialogService {

    /** 未保存確認に対する利用者の選択です。 */
    public enum UnsavedChoice {
        SAVE,
        DISCARD,
        CANCEL
    }

    public UnsavedChoice confirmUnsaved(Window owner) {
        ButtonType save = new ButtonType("保存", ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("破棄", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("キャンセル", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(owner);
        alert.setTitle("未保存の変更");
        alert.setHeaderText("保存されていない変更があります。");
        alert.setContentText("変更を保存してから続行しますか？");
        alert.getButtonTypes().setAll(save, discard, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == save) {
            return UnsavedChoice.SAVE;
        }
        if (result.isPresent() && result.get() == discard) {
            return UnsavedChoice.DISCARD;
        }
        return UnsavedChoice.CANCEL;
    }

    public void showError(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
