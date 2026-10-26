
package com.silo.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SupabaseStore {
    private static final String BASE_URL =
            "https://xjlbqlhghwossvildrze.supabase.co/rest/v1/";

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    public SupabaseStore() {
        this.apiKey = System.getenv("SUPABASE_PUBLISHABLE_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Missing SUPABASE_PUBLISHABLE_KEY environment variable.");
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
}
