package org.example.scan.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

    @FXML
    private void initialize() {
        /* Bindings simples */
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTotalSpace()));
        colFree.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFreeSpace()));

        /* Liste des volumes du système */
        DriveInfo[] drives = Arrays.stream(File.listRoots())
                .map(DriveInfo::new).toArray(DriveInfo[]::new);
        tableDrives.setItems(FXCollections.observableArrayList(drives));

        /* Désactiver le bouton tant qu’aucun disque n’est sélectionné */
        btnScan.disableProperty()
                .bind(tableDrives.getSelectionModel().selectedItemProperty().isNull());
    }

    @FXML
    private void onScan() throws Exception {
        DriveInfo selected = tableDrives.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        /* Ouvrir la fenêtre d’analyse */
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ScanView.fxml"));
        Stage scanStage = new Stage();
        scanStage.setTitle("Scanning " + selected.getName());
        scanStage.setScene(new Scene(loader.load()));

        /* Passer le disque au contrôleur suivant */
        ScanController ctrl = loader.getController();
        ctrl.startScan(selected.getRoot());

        scanStage.show();
        /* (optionnel) Fermer la fenêtre de sélection des disques */
        ((Stage) btnScan.getScene().getWindow()).close();
    }

    @FXML private void onScanFolder() throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder to scan");
        File dir = chooser.showDialog(btnFolder.getScene().getWindow());
        if (dir != null) {
            openScanWindow(dir, dir.getAbsolutePath());
        }
    }

    private void openScanWindow(File root, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ScanView.fxml"));
        Stage stage = new Stage();
        stage.setTitle("Scanning " + title);
        stage.setScene(new Scene(loader.load()));

        ScanController ctrl = loader.getController();
        ctrl.startScan(root);

        stage.show();
        // selon préférence : ne pas fermer la fenêtre principale
    }
}
