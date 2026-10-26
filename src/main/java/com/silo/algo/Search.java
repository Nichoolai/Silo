package com.silo.algo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Linear search over record lists: case-insensitive "contains" match across any of the given text fields. */
public final class Search {
    private Search() { }

    public static <T> List<T> filter(List<T> items, String query, List<Function<T, String>> fields) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<T> out = new ArrayList<>();
        for (T item : items) {
            if (q.isEmpty()) { out.add(item); continue; }
            for (Function<T, String> f : fields) {
                String v = f.apply(item);
                if (v != null && v.toLowerCase().contains(q)) { out.add(item); break; }
            }
        }
        return out;
    }
}
