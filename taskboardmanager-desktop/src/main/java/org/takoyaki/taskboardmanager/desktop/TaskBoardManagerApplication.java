package org.takoyaki.taskboardmanager.desktop;

import java.io.IOException;
import java.net.URL;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.takoyaki.taskboardmanager.desktop.controller.MainController;

/** TaskBoardManagerデスクトップアプリケーションの起動点です。 */
public final class TaskBoardManagerApplication extends Application {

    private static final double INITIAL_WIDTH = 1280.0;
    private static final double INITIAL_HEIGHT = 800.0;

    @Override
    public void start(Stage stage) throws IOException {
        URL fxml = requireResource("/fxml/main-view.fxml");
        FXMLLoader loader = new FXMLLoader(fxml);
        Parent root = loader.load();
        MainController controller = loader.getController();
        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        scene.getStylesheets().add(requireResource("/css/taskboardmanager.css").toExternalForm());
        controller.installSceneShortcuts(scene);

        stage.setTitle("TaskBoardManager");
        stage.setMinWidth(900.0);
        stage.setMinHeight(600.0);
        stage.setScene(scene);
        stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> {
            if (!controller.requestClose()) {
                event.consume();
            }
        });
        stage.show();
    }

    private static URL requireResource(String path) {
        URL resource = TaskBoardManagerApplication.class.getResource(path);
        if (resource == null) {
            throw new IllegalStateException("リソースが見つかりません: " + path);
        }
        return resource;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
