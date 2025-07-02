package dev.kuku.vfl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuku.dto.VflResponse;

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

    // POST Request
    public <T> VflResponse<T> post(String url, Object requestBody, Class<T> responseType) throws Exception {
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

        return objectMapper.readValue(response.body(),
                objectMapper.getTypeFactory().constructParametricType(VflResponse.class, responseType));
    }
}