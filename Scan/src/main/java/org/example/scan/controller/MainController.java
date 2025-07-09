package org.example.scan.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.example.scan.models.DriveInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainController {

    /* ---------- page Drives ---------- */
    @FXML private TableView<DriveInfo> tableDrives;
    @FXML private TableColumn<DriveInfo,String> colName;
    @FXML private TableColumn<DriveInfo,String> colTotal;
    @FXML private TableColumn<DriveInfo,String> colFree;
    @FXML private Button btnScan;
    @FXML private Button btnFolder;
    @FXML private BorderPane pageDrives;

    /* ---------- navigation ---------- */
    @FXML private Button     btnScanPage;
    @FXML private StackPane  contentStack;

    /* ---------- panneau Filter ---------- */
    @FXML private VBox       filterPane;
    @FXML private TextField  txtSize;
    @FXML private ComboBox<String> cbUnit;
    @FXML private CheckBox   chkDocs, chkImgs, chkAudio, chkVideo;
    @FXML private Button     btnApplySize;          // bouton « Apply »

    /* ---------- vue Scan mise en cache ---------- */
    private Parent          scanRoot;
    private ScanController  scanCtrl;
    @FXML private CheckBox chkOther;
// référence du contrôleur Scan

    /* ═════════ INITIALISATION ═════════ */
    @FXML private void initialize() {

        /* === table des disques === */
        colName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getName()));
        colTotal.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getTotalSpace()));
        colFree.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getFreeSpace()));

        DriveInfo[] drives = Arrays.stream(File.listRoots())
                .map(DriveInfo::new).toArray(DriveInfo[]::new);
        tableDrives.setItems(FXCollections.observableArrayList(drives));

        btnScan.disableProperty()
                .bind(tableDrives.getSelectionModel().selectedItemProperty().isNull());
        btnScanPage.setDisable(true);

        /* === filtre taille === */
        cbUnit.getItems().setAll("KB","MB","GB");
        cbUnit.getSelectionModel().select("MB");
        txtSize.setText("100");

        /* listeners qui déclenchent le filtre */
        btnApplySize.setOnAction(e -> applyCurrentFilter());
        txtSize.setOnAction   (e -> applyCurrentFilter());
        cbUnit.setOnAction    (e -> applyCurrentFilter());
        chkDocs.setOnAction   (e -> applyCurrentFilter());
        chkImgs.setOnAction   (e -> applyCurrentFilter());
        chkAudio.setOnAction  (e -> applyCurrentFilter());
        chkVideo.setOnAction  (e -> applyCurrentFilter());
        chkOther.setOnAction(e -> applyCurrentFilter());

    }

    /* ═════════  ACTIONS  ═════════ */

    @FXML private void onScan() throws IOException {
        DriveInfo sel = tableDrives.getSelectionModel().getSelectedItem();
        if (sel != null) openScanPage(sel.getRoot());
    }

    @FXML private void onScanFolder() throws IOException {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose folder to scan");
        File dir = dc.showDialog(btnFolder.getScene().getWindow());
        if (dir != null) openScanPage(dir);
    }

    /* ---------- changement de page ---------- */

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
        applyCurrentFilter();          // garder les réglages actuels
    }

    /* ═════════  LOGIQUE  ═════════ */

    /** Charge (une seule fois) la vue Scan et lance l’analyse. */
    private void openScanPage(File root) throws IOException {

        if (scanRoot == null) {
            FXMLLoader fxml = new FXMLLoader(getClass().getResource("/fxml/ScanView.fxml"));
            scanRoot  = fxml.load();
            scanCtrl  = fxml.getController();
            contentStack.getChildren().add(scanRoot);
        }

        scanCtrl.startScan(root);
        btnScanPage.setDisable(false);
        showScanPage();
    }

    /** Convertit les contrôles du panneau Filter en paramètres et les passe au ScanController. */
    private void applyCurrentFilter() {

        if (scanCtrl == null) return;               // page Scan pas encore affichée

        /* --- seuil taille --- */
        long min = 0;
        try { min = Long.parseLong(txtSize.getText().trim()); }   // valeur saisie
        catch (NumberFormatException ignored) {}

        switch (cbUnit.getValue()) {
            case "GB" -> min *= 1024L * 1024 * 1024;
            case "MB" -> min *= 1024L * 1024;
            case "KB" -> min *= 1024L;
        }

        /* --- types cochés --- */
        Set<String> types = new HashSet<>();
        if (chkDocs.isSelected())  types.add("Documents");
        if (chkImgs.isSelected())  types.add("Images");
        if (chkAudio.isSelected()) types.add("Audio");
        if (chkVideo.isSelected()) types.add("Video");
        if (chkOther.isSelected()) types.add("Other");


        scanCtrl.applyFilter(min, types);
    }

    /* (About inchangé) */
    @FXML private void showAbout() {
        new Alert(Alert.AlertType.INFORMATION,
                "Disk Space Analyzer  –  Version alpha\n© 2025 YourTeam")
                .showAndWait();
    }
}
