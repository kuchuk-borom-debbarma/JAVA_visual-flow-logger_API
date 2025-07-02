package dev.kuku.vfl.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuku.dto.VflResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // GET Request
    public <T> VflResponse<T> get(String url, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .setHeader("User-Agent", "Java HttpClient")
                .setHeader("Accept", "application/json")
                .build();
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(),
                objectMapper.getTypeFactory().constructParametricType(VflResponse.class, responseType));
    }

    // POST Request with improved error handling
    public <T> VflResponse<T> post(String url, Object requestBody, Class<T> responseType) throws Exception {
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url))
                    .setHeader("User-Agent", "Java HttpClient")
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // Check for HTTP error status codes
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP Error: " + response.statusCode() + " - " + response.body());
            }

            // Handle empty response body
            String responseBody = response.body();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("Empty response body received");
            }

            // Use TypeReference for better type handling with generics
            JavaType responseJavaType = objectMapper.getTypeFactory()
                    .constructParametricType(VflResponse.class, responseType);

            return objectMapper.readValue(responseBody, responseJavaType);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request or deserialize response", e);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("HTTP request failed", e);
        }
    }
}