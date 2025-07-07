package org.example.scan.models;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.nio.file.Path;

public class ScanNode {
    private final Path path;
    private final LongProperty size = new SimpleLongProperty();
    private final ObservableList<ScanNode> children = FXCollections.observableArrayList();

    public ScanNode(Path path) { this.path = path; }

    /* -------- getters bindables -------- */
    public Path getPath()  { return path; }
    public long getSize()  { return size.get(); }
    public LongProperty sizeProperty() { return size; }

    public ObservableList<ScanNode> getChildren() { return children; }

    /* utilitaire formaté */
    public String getHumanSize() {
        long b = getSize();
        if (b <= 0) return "0 B";
        String[] u = { "B","KB","MB","GB","TB" };
        int i = (int) (Math.log10(b) / Math.log10(1024));
        return String.format("%.1f %s", b / Math.pow(1024, i), u[i]);
    }
}
