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
                     Consumer<Double> onProgress,          // 0-1
                     BiConsumer<ScanNode,Long> onFinish)  // arbre + durée ms
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

    /* ---------- implémentation interne ---------- */
    private ScanNode buildTree(Path root, Consumer<Double> progress) {
        ScanNode rootNode = new ScanNode(root);
        AtomicLong bytesTotal = new AtomicLong();
        AtomicLong bytesDone  = new AtomicLong();

        /* Estimer l’espace total pour un % approximatif */
        try { bytesTotal.set(Files.getFileStore(root).getTotalSpace()); }
        catch (IOException ignored) {}

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {

                private final ConcurrentLinkedDeque<ScanNode> stack = new ConcurrentLinkedDeque<>();

                /* -- dossiers -- */
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    ScanNode n = new ScanNode(dir);
                    if (!stack.isEmpty()) stack.peek().getChildren().add(n);
                    stack.push(n);
                    return FileVisitResult.CONTINUE;
                }

                /* -- fichiers -- */
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    long size = attrs.size();
                    ScanNode leaf = new ScanNode(file);
                    leaf.sizeProperty().set(size);
                    stack.peek().getChildren().add(leaf);
                    stack.forEach(p -> p.sizeProperty().set(p.getSize() + size));

                    /* progression */
                    double pct = bytesTotal.get() == 0 ? -1 :
                            bytesDone.addAndGet(size) / (double) bytesTotal.get();
                    progress.accept(pct);
                    return FileVisitResult.CONTINUE;
                }

                /* -- dossiers terminés -- */
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    ScanNode done = stack.pop();
                    if (stack.isEmpty()) {          // dir == root
                        rootNode.getChildren().addAll(done.getChildren());
                        rootNode.sizeProperty().set(done.getSize());
                    }
                    return FileVisitResult.CONTINUE;
                }

                /* -- erreurs (droits refusés, etc.) -- */
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // on ignore simplement et on continue
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // log général, mais le scan continue grâce au skip ci-dessus
            e.printStackTrace();
        }

        progress.accept(1.0);   // terminé
        return rootNode;
    }
}
