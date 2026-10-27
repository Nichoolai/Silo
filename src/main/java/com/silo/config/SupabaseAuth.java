package com.silo.config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SupabaseAuth {

    private static final String AUTH_URL =
        "https://nnpxgjnthmyknphjxhwu.supabase.co/auth/v1/token?grant_type=password";

    private final String apiKey;
    private final HttpClient client;
    private final ObjectMapper mapper;

    public SupabaseAuth() {
        apiKey = SupabaseConfig.getPublishableKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "Missing Supabase publishable key."
            );
        }

        client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

        mapper = new ObjectMapper();
    }

    public JsonNode signIn(String email, char[] password)
            throws IOException, InterruptedException {

        if (email == null || email.isBlank()
                || password == null || password.length == 0) {
            throw new IllegalArgumentException(
                "Enter your email and password."
            );
        }

        String passwordText = new String(password);

        try {
            String body = mapper.createObjectNode()
                .put("email", email.trim())
                .put("password", passwordText)
                .toString();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );

            JsonNode result = mapper.readTree(response.body());

            if (response.statusCode() < 200
                    || response.statusCode() >= 300) {

                String error = result.path("msg").asText(
                    result.path("message").asText(
                        result.path("error_description").asText(
                            result.path("error").asText(
                                "No error details returned."
                            )
                        )
                    )
                );

                throw new IOException(
                    "Supabase sign-in failed (HTTP "
                    + response.statusCode() + "): " + error
                );
            }

            if (!result.hasNonNull("access_token")) {
                throw new IOException(
                    "Supabase did not return an access token."
                );
            }

            return result;

        } finally {
            passwordText = null;
            java.util.Arrays.fill(password, '\0');
        }
    }
}