
package com.silo.config;

public final class SupabaseConfig {

    public static final String SUPABASE_URL =
            "https://nnpxgjnthmyknphjxhwu.supabase.co";

    private SupabaseConfig() {
        // Prevent creating instances of this configuration class.
    }

    public static String getPublishableKey() {
        String key = System.getenv("SUPABASE_PUBLISHABLE_KEY");

        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing SUPABASE_PUBLISHABLE_KEY environment variable."
            );
        }

        return key;
    }
}
