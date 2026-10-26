package com.silo.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Minimal tab-separated text persistence. Tabs, newlines and backslashes inside fields are escaped. */
public class FileStore {
    private final Path dir;

    public FileStore(Path dir) {
        this.dir = dir;
        try { Files.createDirectories(dir); } catch (IOException e) { throw new IllegalStateException("Cannot create data folder " + dir, e); }
    }

    public Path getDir() { return dir; }

    public List<String[]> read(String file) {
        Path p = dir.resolve(file);
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(p)) return rows;
        try {
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                if (line.isEmpty()) continue;
                String[] parts = line.split("\t", -1);
                for (int i = 0; i < parts.length; i++) parts[i] = unescape(parts[i]);
                rows.add(parts);
            }
        } catch (IOException e) { throw new IllegalStateException("Cannot read " + p, e); }
        return rows;
    }

    /** Writes to a temp file first, then replaces the real file, so a crash cannot leave a half-written file. */
    public void write(String file, List<String[]> rows) {
        Path p = dir.resolve(file), tmp = dir.resolve(file + ".tmp");
        List<String> lines = new ArrayList<>();
        for (String[] r : rows) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.length; i++) { if (i > 0) sb.append('\t'); sb.append(escape(r[i])); }
            lines.add(sb.toString());
        }
        try {
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { throw new IllegalStateException("Cannot save " + p, e); }
    }

    static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else sb.append(c);
        }
        return sb.toString();
    }
}
