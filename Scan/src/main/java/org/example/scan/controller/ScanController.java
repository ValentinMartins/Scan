package org.example.scan.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import org.example.scan.models.ScanNode;
import org.example.scan.service.DiskScannerService;

import java.nio.file.Path;

public class ScanController {

    @FXML private ProgressBar bar;
    @FXML private TreeTableView<ScanNode> tree;
    @FXML private TreeTableColumn<ScanNode,String> colName;
    @FXML private TreeTableColumn<ScanNode,String> colSize;
    @FXML private Label lblStatus;

    private final DiskScannerService svc = new DiskScannerService();

    public void startScan(java.io.File root) {
        lblStatus.setText("Scanning " + root.getAbsolutePath());
        colName.setCellValueFactory(
                (TreeTableColumn.CellDataFeatures<ScanNode,String> p) ->
                        new ReadOnlyStringWrapper(p.getValue().getValue().getPath().getFileName() == null
                                ? p.getValue().getValue().getPath().toString()
                                : p.getValue().getValue().getPath().getFileName().toString()));
        colSize.setCellValueFactory(
                param -> new ReadOnlyStringWrapper(param.getValue().getValue().getHumanSize()));

        svc.scan(root.toPath(),
                pct -> Platform.runLater(() -> bar.setProgress(
                        pct < 0 ? ProgressIndicator.INDETERMINATE_PROGRESS : pct)),
                (treeRoot, millis) -> Platform.runLater(() -> {
                    tree.setRoot(buildTreeItem(treeRoot));
                    tree.getRoot().setExpanded(true);
                    bar.setProgress(1);
                    lblStatus.setText("Finished in " + millis/1000.0 + " s");
                }));

    }

    private TreeItem<ScanNode> buildTreeItem(ScanNode node) {
        TreeItem<ScanNode> item = new TreeItem<>(node);
        node.getChildren().forEach(child -> item.getChildren().add(buildTreeItem(child)));
        item.getChildren().sort((a,b)->Long.compare(
                b.getValue().getSize(), a.getValue().getSize())); // taille décroissante
        return item;
    }
}
