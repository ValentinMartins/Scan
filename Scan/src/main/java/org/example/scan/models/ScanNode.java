package org.example.scan.models;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Nœud (fichier ou dossier) pour l’arbre d’analyse disque.
 * – conserve les propriétés JavaFX (binding / TableView)
 * – ajoute displayName, isDirectory, stream(), addChild() …
 */
public class ScanNode {

    /* ----------------------- champs ----------------------- */
    private final Path path;
    private final boolean directory;           // dossier ou fichier ?
    private final LongProperty size = new SimpleLongProperty();
    private final ObservableList<ScanNode> children =
            FXCollections.observableArrayList();

    /* ------------------- constructeur --------------------- */
    public ScanNode(Path path) {
        this(path, 0L);
    }
    public ScanNode(Path path, long size) {
        this.path       = path;
        this.directory  = Files.isDirectory(path);
        this.size.set(size);
    }

    /* ------------------- accesseurs ---------------------- */
    public Path getPath()          { return path; }
    public boolean isDirectory()   { return directory; }

    // propriété bindable (table, graphique…)
    public long getSize()          { return size.get(); }
    public void setSize(long s)    { size.set(s); }
    public LongProperty sizeProperty() { return size; }

    public ObservableList<ScanNode> getChildren() { return children; }

    /** Texte à afficher dans la colonne “Name”. */
    public String getDisplayName() {
        return path.getFileName() == null
                ? path.toString()
                : path.getFileName().toString();
    }

    /* ------------- helpers pour le service de scan -------- */

    /** Ajoute un enfant à la liste (appelé depuis DiskScannerService). */
    public void addChild(ScanNode child) { children.add(child); }

    /** Stream récursif : this + tous les descendants. */
    public Stream<ScanNode> stream() {
        return Stream.concat(Stream.of(this),
                children.stream().flatMap(ScanNode::stream));
    }

    /* -------------- utils d’affichage --------------------- */

    public String getHumanSize() {
        long b = getSize();
        if (b <= 0) return "0 B";
        String[] u = { "B","KB","MB","GB","TB" };
        int i = (int) (Math.log10(b) / Math.log10(1024));
        return String.format("%.1f %s", b / Math.pow(1024, i), u[i]);
    }
}
