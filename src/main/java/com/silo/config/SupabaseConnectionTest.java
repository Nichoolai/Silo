
package com.silo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.silo.storage.SupabaseStore;

public class SupabaseConnectionTest {
    public static void main(String[] args) throws Exception {
        SupabaseStore store = new SupabaseStore();

        JsonNode donations = store.get("donations", "select=id&limit=5");

        System.out.println("Supabase connection successful!");
        System.out.println("Donations returned: " + donations.size());
        System.out.println("Data: " + donations.toPrettyString());
    }
}
