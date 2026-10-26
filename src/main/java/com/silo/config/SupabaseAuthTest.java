
package com.silo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.silo.storage.SupabaseStore;
import java.util.Arrays;
import java.util.Scanner;

public class SupabaseAuthTest {
    public static void main(String[] args) throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Supabase test email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Supabase test password: ");
            char[] password = scanner.nextLine().toCharArray();

            JsonNode result;
            try {
                SupabaseAuth auth = new SupabaseAuth();
                result = auth.signIn(email, password);
            } finally {
                Arrays.fill(password, '\0');
            }

            System.out.println("Supabase sign-in successful!");

            String accessToken =
                    result.path("access_token").asText();

            SupabaseStore store = new SupabaseStore();
            JsonNode profile = store.getMyProfile(accessToken);

            System.out.println("Profile retrieved successfully!");
            System.out.println("Name: "
                    + profile.path("full_name").asText());
            System.out.println("Email: "
                    + profile.path("email").asText());
            System.out.println("Role: "
                    + profile.path("role").asText());
            System.out.println("Status: "
                    + profile.path("status").asText());
        }
    }
}
