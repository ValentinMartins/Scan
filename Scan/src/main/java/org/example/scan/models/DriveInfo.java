package org.example.scan.models;

import java.io.File;
import java.text.NumberFormat;

public class DriveInfo {
    private final File root;

    public DriveInfo(File root) { this.root = root; }

    /* Getters utilitaires pour TableView */
    public String getName()        { return root.getAbsolutePath(); }
    public String getTotalSpace()  { return human(root.getTotalSpace()); }
    public String getFreeSpace()   { return human(root.getFreeSpace()); }
    public File   getRoot()        { return root; }

    private String human(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = { "B","KB","MB","GB","TB","PB" };
        int idx = (int) (Math.log10(bytes) / Math.log10(1024));
        return NumberFormat.getInstance().format(
                bytes / Math.pow(1024, idx)) + " " + units[idx];
    }
}

