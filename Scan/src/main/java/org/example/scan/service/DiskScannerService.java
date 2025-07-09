package org.example.scan.service;

import org.example.scan.models.ScanNode;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DiskScannerService {

    private final ExecutorService pool = ForkJoinPool.commonPool();

    public void scan(Path root,
                     Consumer<Double>           onProgress,   // 0-1 ou –1 indéterminé
                     BiConsumer<ScanNode,Long> onFinish)     // arbre + durée (ms)
    {
        long start = System.currentTimeMillis();

        CompletableFuture
                .supplyAsync(() -> buildTree(root, onProgress), pool)
                .whenComplete((tree, err) -> {
                    long dur = System.currentTimeMillis() - start;
                    if (err != null) err.printStackTrace();
                    onFinish.accept(tree, dur);
                });
    }

    /* ---------------------------------------------------------------------- */
    private ScanNode buildTree(Path root, Consumer<Double> progress) {

        /* ───────── estimation pour la progression ───────── */
        AtomicLong bytesTotal = new AtomicLong();
        AtomicLong bytesDone  = new AtomicLong();
        try { bytesTotal.set(Files.getFileStore(root).getTotalSpace()); }
        catch (IOException ignored) {}

        /* ───────── parcours profondeur-d’abord ───────── */
        ScanNode[] rootHolder = new ScanNode[1];                   // ref finale
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {

                private final ConcurrentLinkedDeque<ScanNode> stack = new ConcurrentLinkedDeque<>();

                /* ── avant d’entrer dans un dossier ────────────────────────── */
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    ScanNode dirNode = new ScanNode(dir, 0L);      // taille 0 temporaire
                    if (!stack.isEmpty()) stack.peek().addChild(dirNode);
                    stack.push(dirNode);

                    return FileVisitResult.CONTINUE;
                }

                /* ── fichier rencontré ─────────────────────────────────────── */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    long size = attrs.size();

                    // nœud feuille
                    ScanNode fileNode = new ScanNode(file, size);
                    stack.peek().addChild(fileNode);               // dans le dossier courant

                    // propager la taille au(x) dossier(s) parents
                    stack.forEach(n -> n.setSize(n.getSize() + size));

                    // progression
                    double pct = bytesTotal.get() == 0 ? -1
                            : bytesDone.addAndGet(size) / (double) bytesTotal.get();
                    progress.accept(pct);

                    return FileVisitResult.CONTINUE;
                }

                /* ── après avoir quitté un dossier ─────────────────────────── */
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    ScanNode finished = stack.pop();
                    if (stack.isEmpty()) {             // c’était la racine
                        rootHolder[0] = finished;
                    }
                    return FileVisitResult.CONTINUE;
                }

                /* ── erreurs d’accès : on ignore ───────────────────────────── */
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();   // log global, le scan continue quand même
        }

        progress.accept(1.0);      // 100 %
        return rootHolder[0] != null
                ? rootHolder[0]
                : new ScanNode(root, 0L);   // sécurité
    }
}
