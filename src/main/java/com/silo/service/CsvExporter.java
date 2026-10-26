package com.silo.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** RFC 4180-style CSV writer (quotes fields containing commas, quotes or newlines). Adds a UTF-8 BOM so Excel opens it correctly. */
public final class CsvExporter {
    private CsvExporter() { }

    public static void write(Path file, String[] header, List<String[]> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(line(header));
        for (String[] r : rows) lines.add(line(r));
        try { Files.writeString(file, "\uFEFF" + String.join("\r\n", lines) + "\r\n", StandardCharsets.UTF_8); }
        catch (IOException e) { throw new ValidationException("Could not write " + file + ": " + e.getMessage()); }
    }

    static String line(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            String f = fields[i] == null ? "" : fields[i];
            if (f.matches("(?s).*[\",\\r\\n].*")) f = "\"" + f.replace("\"", "\"\"") + "\"";
            sb.append(f);
        }
        return sb.toString();
    }
}
