package org.example.scan.controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.Group;
import org.example.scan.models.ScanNode;
import org.example.scan.service.DiskScannerService;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.nio.file.Files;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Predicate;   // s’il n’est pas déjà présent


/**
 * Contrôleur de la vue ScanView.fxml.
 */
public class ScanController {

    /* ────────────────  FXML  ──────────────── */
    @FXML private TableColumn<ScanNode, ScanNode> colDelete;
    @FXML private ProgressBar                      bar;
    @FXML private TreeTableView<ScanNode>          tree;
    @FXML private TreeTableColumn<ScanNode,String> colName;
    @FXML private TreeTableColumn<ScanNode,String> colSize;

    // Top-20
    @FXML private TableView<ScanNode>          tblTop;
    @FXML private TableColumn<ScanNode,String> colRank;
    @FXML private TableColumn<ScanNode,String> colNameTop;
    @FXML private TableColumn<ScanNode,String> colSizeTop;
    @FXML private TableColumn<ScanNode, ScanNode> colColor;

    @FXML private PieChart pie;
    @FXML private Label    lblStatus;
    @FXML private TableColumn<ScanNode,String> colModTop;
    private ScanNode masterRoot;
    private long lastMinSize = 0;                      // seuil en octets
    private Set<String> lastAllowedTypes =
            new HashSet<>(Set.of("Documents","Images","Audio","Video"));

    /* ───────── services / format ───────── */
    private final DiskScannerService svc   = new DiskScannerService();
    private final DecimalFormat      human = new DecimalFormat("#,##0.#");
    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static String lastModified(Path p) {
        try {
            FileTime ft = Files.getLastModifiedTime(p);
            return LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault())
                    .format(DT_FMT);
        } catch (IOException e) {
            return "";
        }
    }

    /* ───────── icons cache (16 px) ───────── */
    private static final Map<String, Image> ICON_CACHE = new HashMap<>();
    private static Image loadIcon(String path) {
        return ICON_CACHE.computeIfAbsent(path, p ->
                new Image(Objects.requireNonNull(ScanController.class.getResource(p)).toString(),
                        16, 16, true, true));
    }

    private static final Image ICO_FOLDER = loadIcon("/icons/folder.png");

    @FXML
    private void initialize() {
        initDeleteColumn();
    }

    private static final Map<String,String> EXT_ICON = Map.ofEntries(
            Map.entry("png", "/icons/image.png"), Map.entry("jpg", "/icons/image.png"),
            Map.entry("jpeg","/icons/image.png"), Map.entry("gif", "/icons/image.png"),
            Map.entry("bmp", "/icons/image.png"), Map.entry("tiff","/icons/image.png"),
            Map.entry("heic","/icons/image.png"),

            Map.entry("mp4","/icons/video.png"),  Map.entry("mkv","/icons/video.png"),
            Map.entry("avi","/icons/video.png"),  Map.entry("mov","/icons/video.png"),
            Map.entry("wmv","/icons/video.png"),  Map.entry("flv","/icons/video.png"),
            Map.entry("webm","/icons/video.png"),

            Map.entry("mp3","/icons/audio.png"),  Map.entry("wav","/icons/audio.png"),
            Map.entry("flac","/icons/audio.png"), Map.entry("ogg","/icons/audio.png"),
            Map.entry("aac","/icons/audio.png"),  Map.entry("opus","/icons/audio.png"),

            Map.entry("zip","/icons/installer.png"), Map.entry("rar","/icons/installer.png"),
            Map.entry("7z","/icons/installer.png"),  Map.entry("tar","/icons/installer.png"),
            Map.entry("gz","/icons/installer.png"),  Map.entry("pkg","/icons/installer.png"),
            Map.entry("dmg","/icons/installer.png"), Map.entry("exe","/icons/exe.png"),
            Map.entry("msi","/icons/exe.png"),

            Map.entry("pdf","/icons/pdf.png"),  Map.entry("xls","/icons/excel.png"),
            Map.entry("xlsx","/icons/excel.png"),Map.entry("csv","/icons/excel.png"),
            Map.entry("json","/icons/json.png"),

            Map.entry("db","/icons/database.png"), Map.entry("sqlite","/icons/database.png"),
            Map.entry("mdb","/icons/database.png"),Map.entry("parquet","/icons/database.png"),

            Map.entry("obj","/icons/3d.png"),   Map.entry("fbx","/icons/3d.png"),
            Map.entry("stl","/icons/3d.png"),

            Map.entry("java","/icons/code.png"),Map.entry("kt","/icons/code.png"),
            Map.entry("py","/icons/code.png"),  Map.entry("js","/icons/code.png"),
            Map.entry("ts","/icons/code.png"),  Map.entry("cpp","/icons/code.png"),
            Map.entry("c","/icons/code.png"),   Map.entry("cs","/icons/code.png"),
            Map.entry("go","/icons/code.png"),  Map.entry("rs","/icons/code.png"),
            Map.entry("sh","/icons/code.png"),  Map.entry("ps1","/icons/code.png")
    );

    /** Choix de l’icône en fonction de l’extension. */
    private static Image iconFor(ScanNode n) {
        if (n.isDirectory()) return ICO_FOLDER;
        return loadIcon(EXT_ICON.getOrDefault(extension(n.getPath()), "/icons/file.png"));
    }

    /* Couleurs par type (camembert) */
    private static final Map<String, Color> TYPE_COLOR = Map.of(
            "Video",     Color.web("#f87171"),
            "Documents", Color.web("#facc15"),
            "Images",    Color.web("#34d399"),
            "Audio",     Color.web("#38bdf8"),
            "Other",     Color.web("#6366f1")
    );

    /* ════════════════════════════════════════ */
    /*              PUBLIC  API                */
    /* ════════════════════════════════════════ */
    public void startScan(java.io.File root) {

        resetUI();
        lblStatus.setText("Scanning " + root.getAbsolutePath());

        /* ------------- colonnes TreeTableView ------------- */
        colName.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getValue().getDisplayName()));
        colSize.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(humanSize(cd.getValue().getValue().getSize())));
        colSize.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        colName.setCellFactory(c -> new TreeTableCell<>() {
            private final ImageView view = new ImageView();
            @Override protected void updateItem(String txt, boolean empty) {
                super.updateItem(txt, empty);
                if (empty || getTreeTableRow().getItem() == null) { setText(null); setGraphic(null); }
                else {
                    view.setImage(iconFor(getTreeTableRow().getItem()));
                    setText(txt); setGraphic(view);
                }
            }
        });

        /* ------------- scan asynchrone ------------- */
        svc.scan(root.toPath(),
                pct -> Platform.runLater(() ->
                        bar.setProgress(pct < 0 ? ProgressIndicator.INDETERMINATE_PROGRESS : pct)),
                (treeRoot, ms) -> Platform.runLater(() -> {
                    masterRoot = treeRoot;
                    tree.setRoot(buildTreeItem(treeRoot));
                    tree.getRoot().setExpanded(true);

                    long totalFiles = treeRoot.stream().filter(sn -> !sn.isDirectory()).count();
                    long totalDirs  = treeRoot.stream().filter(ScanNode::isDirectory).count() - 1;

                    if (colDelete.getCellFactory() == null) initDeleteColumn();
                    feedStats(treeRoot);

                    bar.setProgress(1);
                    lblStatus.setText("Finished in " + human.format(ms / 1000.0) + " s — " +
                            totalFiles + " Files, " + totalDirs + " Folders");
                }));
    }

    /* ════════════════════════════════════════ */
    /*               HELPERS                   */
    /* ════════════════════════════════════════ */
    private void resetUI() {
        tree.setRoot(null);
        pie.setData(FXCollections.emptyObservableList());
        tblTop.setItems(FXCollections.emptyObservableList());
        bar.setVisible(true); bar.setManaged(true); bar.setProgress(0);
    }

    private static TreeItem<ScanNode> buildTreeItem(ScanNode n) {
        TreeItem<ScanNode> it = new TreeItem<>(n);
        n.getChildren().stream()
                .sorted(Comparator.comparingLong(ScanNode::getSize).reversed())
                .forEach(child -> it.getChildren().add(buildTreeItem(child)));
        return it;
    }

    /* ─────────────────────  STATISTIQUES  ───────────────────── */
    private void feedStats(ScanNode root) {

        /* Top-20 fichiers */
        List<ScanNode> top = root.stream()
                .filter(sn -> !sn.isDirectory())
                .sorted(Comparator.comparingLong(ScanNode::getSize).reversed())
                .limit(20)
                .toList();
        tblTop.setItems(FXCollections.observableArrayList(top));

        colRank.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String txt, boolean empty) {
                super.updateItem(txt, empty);
                setText(empty ? null : "%02d".formatted(getIndex() + 1));
            }
        });

        /* Colonne carré couleur dans Top-20 */
        colColor.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colColor.setCellFactory(col -> new TableCell<>() {
            private final Region square = new Region();
            { square.setPrefSize(12, 12); setAlignment(Pos.CENTER); }
            @Override protected void updateItem(ScanNode sn, boolean empty) {
                super.updateItem(sn, empty);
                if (empty || sn == null) setGraphic(null);
                else {
                    square.setStyle("-fx-background-color:" + toRgb(TYPE_COLOR.get(matchType(sn.getPath()))) +
                            "; -fx-border-color:#333; -fx-border-radius:1;");
                    setGraphic(square);
                }
            }
        });
        colColor.setPrefWidth(26);

        /* Nom + icône dans Top-20 */
        colNameTop.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDisplayName()));
        colModTop.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(lastModified(cd.getValue().getPath())));
        colModTop.setStyle("-fx-alignment: CENTER-RIGHT;");
        colNameTop.setCellFactory(col -> new TableCell<>() {
            private final ImageView view = new ImageView();
            @Override protected void updateItem(String txt, boolean empty) {
                super.updateItem(txt, empty);
                if (empty || getTableRow().getItem() == null) { setText(null); setGraphic(null); }
                else { view.setImage(iconFor(getTableRow().getItem())); setText(txt); setGraphic(view); }
            }
        });

        colSizeTop.setCellValueFactory(cd -> new ReadOnlyStringWrapper(humanSize(cd.getValue().getSize())));
        colSizeTop.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        tblTop.setRowFactory(tv -> {
            TableRow<ScanNode> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty())
                    try { Desktop.getDesktop().open(row.getItem().getPath().getParent().toFile()); }
                    catch (IOException ignored) {}
            });
            return row;
        });

        /* Buckets : taille + nombre fichiers */
        Map<String, Long> sizeBuckets  = new LinkedHashMap<>();
        Map<String, Long> countBuckets = new LinkedHashMap<>();
        TYPE_COLOR.keySet().forEach(t -> { sizeBuckets.put(t,0L); countBuckets.put(t,0L); });

        root.stream().filter(sn -> !sn.isDirectory()).forEach(sn -> {
            String t = matchType(sn.getPath());
            sizeBuckets.compute(t,(k,v)->v+sn.getSize());
            countBuckets.compute(t,(k,v)->v+1);
        });

        long total = root.getSize();

        /* PieChart.Data */
        List<PieChart.Data> data = sizeBuckets.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new PieChart.Data("%s (%.1f %%)".formatted(e.getKey(), e.getValue()*100.0/total),
                        e.getValue()))
                .toList();

        // min 3 % visuel
        data.forEach(d -> {
            if (d.getPieValue()/total < 0.03) d.setPieValue(total*0.03);
        });
        pie.setData(FXCollections.observableArrayList(data));

        /* ---------- couleurs + étiquettes ---------- */
        stylisePie(data, countBuckets);

    }

    /* ──────────── Utils ──────────── */
    private static String matchType(Path p) {
        return switch (extension(p)) {
            case "pdf","doc","docx","txt","xls","xlsx","ppt","pptx","csv","json" -> "Documents";
            case "png","jpg","jpeg","gif","bmp","tiff","heic","svg","webp"       -> "Images";
            case "mp3","wav","flac","ogg","aac","opus"                           -> "Audio";
            case "mp4","mkv","avi","mov","wmv","flv","webm"                      -> "Video";
            default -> "Other";
        };
    }
    private static String matchTypeLabel(String pie) { return pie.split(" ")[0]; }

    private static String humanSize(long b) {
        double v=b; String[] u={"B","KB","MB","GB","TB"}; int i=0;
        while(v>=1024&&i<u.length-1){v/=1024;i++;}
        return new DecimalFormat("#,##0.#").format(v)+' '+u[i];
    }
    private static String extension(Path p) {
        String s = p.getFileName().toString();
        int i = s.lastIndexOf('.');
        return (i < 0 ? "" : s.substring(i + 1)).toLowerCase();
    }
    private static String toRgb(Color c) {
        return "#%02X%02X%02X".formatted(
                (int) Math.round(c.getRed()   * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue()  * 255));
    }

    /** Colonne « Supprimer » avec bouton et confirmation. */
    /** Colonne « Supprimer » avec bouton et confirmation. */
    private void initDeleteColumn() {

        colDelete.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));

        colDelete.setCellFactory(col -> new TableCell<ScanNode, ScanNode>() {

            private final Button btn = new Button("🗑");   // ou une ImageView

            {
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            }

            @Override protected void updateItem(ScanNode sn, boolean empty) {
                super.updateItem(sn, empty);

                if (empty || sn == null) {
                    setGraphic(null);
                    return;
                }

                btn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Supprimer « " + sn.getDisplayName() + " » ?", ButtonType.OK, ButtonType.CANCEL);
                    confirm.setHeaderText(null);

                    confirm.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.OK) {
                            try {
                                Files.deleteIfExists(sn.getPath());

                                ScanNode rootNode = (ScanNode) tree.getRoot().getValue();

                                removeNode(masterRoot, sn);
                                tblTop.getItems().remove(getIndex());
                                applyFilter(lastMinSize, lastAllowedTypes);
                                tree.setRoot(buildTreeItem(rootNode));
                                tree.getRoot().setExpanded(true);

                                feedStats(rootNode);
                            } catch (IOException ex) {
                                new Alert(Alert.AlertType.ERROR,
                                        "Échec de la suppression : " + ex.getMessage()).showAndWait();
                            }
                        }
                    });
                });

                setGraphic(btn);
                setAlignment(Pos.CENTER);
            }
        });

        colDelete.setSortable(false);
        colDelete.setReorderable(false);
    }

    private boolean removeNode(ScanNode parent, ScanNode target) {
        if (parent.getChildren().remove(target)) return true;
        for (ScanNode child : parent.getChildren())
            if (removeNode(child, target)) return true;
        return false;
    }

    /** (interne) teste si un nœud respecte le filtre courant */
    private boolean keepNode(ScanNode n, long minBytes, Set<String> types) {
        if (n.isDirectory())
            return n.getChildren().stream().anyMatch(ch -> keepNode(ch, minBytes, types));
        return n.getSize() >= minBytes && types.contains(matchType(n.getPath()));
    }

    /** reconstruit un TreeItem filtré */
    private TreeItem<ScanNode> buildTreeItemFiltered(ScanNode n,
                                                     long minBytes,
                                                     Set<String> types) {
        if (!keepNode(n, minBytes, types)) return null;
        TreeItem<ScanNode> it = new TreeItem<>(n);
        if (n.isDirectory())
            n.getChildren().forEach(ch -> {
                TreeItem<ScanNode> sub = buildTreeItemFiltered(ch, minBytes, types);
                if (sub != null) it.getChildren().add(sub);
            });
        return it;
    }

    /** ► appelée par MainController ; mémorise les valeurs pour ré-utiliser après suppression */
    public void applyFilter(long minBytes, Set<String> types) {

        lastMinSize = minBytes;
        lastAllowedTypes = new HashSet<>(types);

        ScanNode rootNode = masterRoot;
        if (rootNode == null) return;

        // ----------- Arbre ----------
        TreeItem<ScanNode> filteredRoot = buildTreeItemFiltered(rootNode, minBytes, types);
        tree.setRoot(filteredRoot);
        if (filteredRoot != null) filteredRoot.setExpanded(true);

        // ----------- Top-20 ----------
        List<ScanNode> top = rootNode.stream()
                .filter(sn -> !sn.isDirectory())
                .filter(sn -> keepNode(sn, minBytes, types))
                .sorted(Comparator.comparingLong(ScanNode::getSize).reversed())
                .limit(20).toList();
        tblTop.setItems(FXCollections.observableArrayList(top));

        // ----------- Camembert ----------
        Map<String, Long> sizeBuckets = new LinkedHashMap<>();
        Map<String, Long> countBuckets= new LinkedHashMap<>();
        TYPE_COLOR.keySet().forEach(t -> { sizeBuckets.put(t,0L); countBuckets.put(t,0L); });

        rootNode.stream().filter(sn -> !sn.isDirectory())
                .filter(sn -> keepNode(sn, minBytes, types))
                .forEach(sn -> {
                    String t = matchType(sn.getPath());
                    sizeBuckets.compute(t,(k,v)->v+sn.getSize());
                    countBuckets.compute(t,(k,v)->v+1);
                });

        long total = sizeBuckets.values().stream().mapToLong(Long::longValue).sum();
        List<PieChart.Data> data = sizeBuckets.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> new PieChart.Data("%s (%.1f %%)".formatted(
                        e.getKey(), e.getValue()*100.0/Math.max(1,total)), e.getValue()))
                .toList();
        pie.setData(FXCollections.observableArrayList(data));

        stylisePie(data, countBuckets);

    }

    /** Applique couleurs + petites étiquettes au camembert */
    private void stylisePie(List<PieChart.Data> data,
                            Map<String,Long> countBuckets) {

        Platform.runLater(() -> {
            pie.applyCss(); pie.layout();               // assure les nœuds

            var symbols = pie.lookupAll(".chart-legend-item-symbol").stream().toList();

            for (int i = 0; i < data.size(); i++) {

                PieChart.Data d   = data.get(i);
                String        typ = matchTypeLabel(d.getName());
                Color         col = TYPE_COLOR.get(typ);
                String        rgb = toRgb(col);

                /* couleur du secteur + légende */
                d.getNode().setStyle("-fx-pie-color:" + rgb + ';');
                if (i < symbols.size())
                    symbols.get(i).setStyle("-fx-background-color:" + rgb + ';');

                /* étiquette (nb fichiers) au centre de la part */
                long   nb   = countBuckets.getOrDefault(typ, 0L);
                Text   lbl  = new Text(String.valueOf(nb));
                lbl.setMouseTransparent(true);
                lbl.setStyle("-fx-font-weight:bold;");
                lbl.setFill(col.getBrightness() > .6 ? Color.BLACK : Color.WHITE);

                Node slice = d.getNode();
                Parent g   = slice.getParent();
                while (g != null && !(g instanceof Group)) g = g.getParent();
                if (g instanceof Group grp) grp.getChildren().add(lbl);

                Runnable place = () -> {
                    Bounds b = slice.getBoundsInParent();
                    lbl.setLayoutX(b.getMinX() + b.getWidth()/2 - lbl.getBoundsInLocal().getWidth()/2);
                    lbl.setLayoutY(b.getMinY() + b.getHeight()/2 + lbl.getBoundsInLocal().getHeight()/4);
                };
                place.run();
                slice.boundsInParentProperty().addListener((o,oldV,newV)->place.run());
            }
        });
    }




}