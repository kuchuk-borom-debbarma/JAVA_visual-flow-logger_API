package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import lombok.RequiredArgsConstructor;
import org.javatuples.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class VFLHub implements VFLFlushHandler {
    private final URI url;
    private final HttpClient httpClient = HttpClient
            .newBuilder()
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        try {
            String jsonBody = objectMapper.writeValueAsString(logs);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/logs"))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        try {
            String jsonBody = objectMapper.writeValueAsString(blocks);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/blocks"))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean pushBlockStartsToServer(Map<String, Long> blockStarts) {
        try {
            String jsonBody = objectMapper.writeValueAsString(blockStarts);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/block-starts"))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean pushBlockEndsToServer(Map<String, Pair<Long, String>> blockEnds) {
        try {
            String jsonBody = objectMapper.writeValueAsString(blockEnds);
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/blocks-ends"))
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void closeFlushHandler() {
    }
}