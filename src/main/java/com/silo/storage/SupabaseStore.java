package com.silo.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.silo.config.SupabaseConfig;

public class SupabaseStore {
    private static final String BASE_URL =
            "https://nnpxgjnthmyknphjxhwu.supabase.co/rest/v1/";

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    public SupabaseStore() {
        this.apiKey = SupabaseConfig.getPublishableKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Missing Supabase publishable key.");
        }

        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        this.mapper = new ObjectMapper();
    }

    public JsonNode get(String table, String query)
            throws IOException, InterruptedException {
        String url = BASE_URL + table + "?";
        if (query != null && !query.isBlank()) {
            url += query;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase error (" + response.statusCode()
                    + "): " + response.body());
        }

        return mapper.readTree(response.body());
    }

    public JsonNode getMyProfile(String accessToken)
            throws IOException, InterruptedException {

        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "A Supabase access token is required.");
        }

        String url = BASE_URL
                + "profiles?select=id,full_name,email,role,status&limit=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            throw new IOException(
                    "Supabase profile lookup failed (HTTP "
                            + response.statusCode() + ").");
        }

        JsonNode profiles = mapper.readTree(response.body());

        // Self-healing: If profile row is missing, fetch Auth user details, include the UUID, and insert it safely under RLS!
        if (!profiles.isArray() || profiles.isEmpty()) {
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://nnpxgjnthmyknphjxhwu.supabase.co/auth/v1/user"))
                    .timeout(Duration.ofSeconds(20))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());
            if (userResponse.statusCode() >= 200 && userResponse.statusCode() < 300) {
                JsonNode userNode = mapper.readTree(userResponse.body());
                String userId = userNode.path("id").asText(null);
                String email = userNode.path("email").asText("user@silo.com");
                String fullName = email.contains("@") ? email.split("@")[0] : "User";

                ObjectNode profileBody = mapper.createObjectNode();
                if (userId != null && !userId.isBlank()) {
                    profileBody.put("id", userId); // Matches auth.users(id) for RLS compliance
                }
                profileBody.put("email", email);
                profileBody.put("full_name", fullName);
                profileBody.put("role", "FARMER");
                profileBody.put("status", "ACTIVE");

                HttpRequest insertRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "profiles"))
                        .timeout(Duration.ofSeconds(20))
                        .header("apikey", apiKey)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=representation")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(profileBody)))
                        .build();

                HttpResponse<String> insertResponse = client.send(insertRequest, HttpResponse.BodyHandlers.ofString());
                JsonNode newProfiles = mapper.readTree(insertResponse.body());
                if (newProfiles.isArray() && !newProfiles.isEmpty()) {
                    return newProfiles.get(0);
                }
            }

            throw new IOException("No profile was found for this account.");
        }

        return profiles.get(0);
    }

    public void createProfileIfMissing(String accessToken, String email, String name, String role, String status)
            throws IOException, InterruptedException {
        String url = BASE_URL + "profiles";

        ObjectNode profileBody = mapper.createObjectNode();
        profileBody.put("email", email);
        profileBody.put("full_name", name);
        profileBody.put("role", role);
        profileBody.put("status", status);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(profileBody)))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}