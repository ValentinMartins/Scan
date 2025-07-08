package org.example.scan;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Scene scene = new Scene(
                FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml")));
        scene.getStylesheets()
                .add(getClass().getResource("/css/dark-theme.css").toExternalForm());

        stage.setTitle("Disk Space Analyzer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
