
package com.silo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.silo.config.SupabaseConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SupabaseAuthService {

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    public SupabaseAuthService() {
        this.apiKey = SupabaseConfig.getPublishableKey();
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.mapper = new ObjectMapper();
    }

    private JsonNode post(String endpoint, JsonNode body)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        SupabaseConfig.SUPABASE_URL + endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("apikey", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        JsonNode result = mapper.readTree(response.body());

        if (response.statusCode() < 200
                || response.statusCode() >= 300) {
            String message = result.path("msg").asText(
                    result.path("message").asText(
                    result.path("error_description").asText(
                    "Supabase authentication failed.")));

            throw new IllegalArgumentException(message);
        }

        return result;
    }
}
