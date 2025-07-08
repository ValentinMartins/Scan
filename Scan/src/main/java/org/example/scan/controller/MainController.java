package org.example.scan.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.example.scan.models.DriveInfo;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class MainController {

    @FXML private TableView<DriveInfo> tableDrives;
    @FXML private TableColumn<DriveInfo,String> colName;
    @FXML private TableColumn<DriveInfo,String> colTotal;
    @FXML private TableColumn<DriveInfo,String> colFree;
    @FXML private Button btnScan;
    @FXML private Button btnFolder;
    @FXML private BorderPane pageDrives;
    @FXML private VBox filterPane;
    @FXML private StackPane contentStack;
    @FXML private ScanController scanController;
    @FXML private Parent scanRoot;
    @FXML private Button btnScanPage;

    @FXML
    private void initialize() {
        /* Bindings simples */
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTotalSpace()));
        colFree.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFreeSpace()));
        btnScanPage.setDisable(true);
        DriveInfo[] drives = Arrays.stream(File.listRoots())
                .map(DriveInfo::new).toArray(DriveInfo[]::new);
        tableDrives.setItems(FXCollections.observableArrayList(drives));

        btnScan.disableProperty()
                .bind(tableDrives.getSelectionModel().selectedItemProperty().isNull());
    }

    @FXML
    private void onScan() throws IOException {
        DriveInfo selected = tableDrives.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openScanPage(selected.getRoot(), selected.getName());
        }
    }

    @FXML
    private void onScanFolder() throws IOException {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose folder to scan");
        File dir = dc.showDialog(btnFolder.getScene().getWindow());
        if (dir != null) openScanPage(dir, dir.getAbsolutePath());
    }


    private void openScanPage(File root, String title) throws IOException {

        if (scanController == null) {
            FXMLLoader fxml = new FXMLLoader(
                    getClass().getResource("/fxml/ScanView.fxml"));
            scanRoot = fxml.load();
            scanController = fxml.getController();
            contentStack.getChildren().add(scanRoot);
        }

        scanController.startScan(root);
        btnScanPage.setDisable(false);
        scanRoot.toFront();
        showScanPage();
    }

    @FXML private void showDrivePage() {
        if (scanRoot != null) {
            scanRoot.setVisible(false);
            scanRoot.setManaged(false);
        }

        pageDrives.setVisible(true);
        pageDrives.setManaged(true);
        pageDrives.toFront();
    }

    @FXML private void showScanPage() {
        if (scanRoot == null) return;

        pageDrives.setVisible(false);
        pageDrives.setManaged(false);

        scanRoot.setVisible(true);
        scanRoot.setManaged(true);
        scanRoot.toFront();
    }

    @FXML private void showAbout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Disk Space Analyzer");
        a.setContentText("Version alpha – mock-up.\n© 2025 YourTeam");
        a.showAndWait();
    }
}
