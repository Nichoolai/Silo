
package com.silo.config;

public final class SupabaseConfig {

    private SupabaseConfig() {
        // Prevent creating instances of this class.
    }

    public static final String SUPABASE_URL =
            "https://xjlbqlhghwossvildrze.supabase.co";

    public static String getPublishableKey() {
        String key = System.getenv("SUPABASE_PUBLISHABLE_KEY");

        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Missing SUPABASE_PUBLISHABLE_KEY environment variable."
            );
        }

        return key;
    }

    public static String getRestUrl() {
        return SUPABASE_URL + "/rest/v1/";
    }
}
