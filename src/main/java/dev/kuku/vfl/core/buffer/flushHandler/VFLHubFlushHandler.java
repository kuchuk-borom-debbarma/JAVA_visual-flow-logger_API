package dev.kuku.vfl.core.buffer.flushHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * VFL Hub flush handler with blocking HttpClient. This is because the order at which the requests are made may or may not be important.
 * If used with AsyncBuffer this will not block main thread as it will be running in provided executor
 */
@Slf4j
@RequiredArgsConstructor
public class VFLHubFlushHandler implements VFLFlushHandler {
    private static final String ADD_LOGS_EP = "/logs";
    private static final String ADD_BLOCKS_EP = "/blocks";
    private static final String ADD_BLOCK_STARTS_EP = "/block-starts";
    private static final String ADD_BLOCK_ENDS_EP = "/block-ends";
    private static final String API_VERSION = "api/v1";
    private final URI url;
    private final HttpClient httpClient = HttpClient
            .newBuilder()
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        log.debug("Attempting to push {} logs to server", logs.size());
        try {
            String jsonBody = objectMapper.writeValueAsString(logs);
            log.trace("Serialized logs to JSON: {} characters", jsonBody.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/" + API_VERSION + ADD_LOGS_EP))
                    .header("Content-Type", "application/json")
                    .build();

            log.debug("Sending POST request to: {}", url + "/" + API_VERSION + ADD_LOGS_EP);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.debug("Successfully pushed logs to server. Status: {}", response.statusCode());
            } else {
                log.warn("Failed to push logs to server. Status: {}, Response: {}", response.statusCode(), response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing logs to server", e);
            log.error(e.fillInStackTrace().toString());
            return false;
        }
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        log.debug("Attempting to push {} blocks to server", blocks.size());
        try {
            String jsonBody = objectMapper.writeValueAsString(blocks);
            log.trace("Serialized blocks to JSON: {} characters", jsonBody.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/" + API_VERSION + ADD_BLOCKS_EP))
                    .header("Content-Type", "application/json")
                    .build();

            log.debug("Sending POST request to: {}", url + "/" + API_VERSION + ADD_BLOCKS_EP);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.debug("Successfully pushed blocks to server. Status: {}", response.statusCode());
            } else {
                log.warn("Failed to push blocks to server. Status: {}, Response: {}", response.statusCode(), response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing blocks to server", e);
            log.error(e.fillInStackTrace().toString());
            return false;
        }
    }

    @Override
    public boolean pushBlockStartsToServer(Map<String, Long> blockStarts) {
        log.debug("Attempting to push {} block starts to server", blockStarts.size());
        try {
            String jsonBody = objectMapper.writeValueAsString(blockStarts);
            log.trace("Serialized block starts to JSON: {} characters", jsonBody.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/" + API_VERSION + ADD_BLOCK_STARTS_EP))
                    .header("Content-Type", "application/json")
                    .build();

            log.debug("Sending POST request to: {}", url + "/" + API_VERSION + ADD_BLOCK_STARTS_EP);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.debug("Successfully pushed block starts to server. Status: {}", response.statusCode());
            } else {
                log.warn("Failed to push block starts to server. Status: {}, Response: {}", response.statusCode(), response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing block starts to server", e);
            log.error(e.fillInStackTrace().toString());
            return false;
        }
    }

    @Override
    public boolean pushBlockEndsToServer(Map<String, BlockEndData> blockEnds) {
        log.debug("Attempting to push {} block ends to server", blockEnds.size());
        try {
            String jsonBody = objectMapper.writeValueAsString(blockEnds);
            log.trace("Serialized block ends to JSON: {} characters", jsonBody.length());

            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(url + "/" + API_VERSION + ADD_BLOCK_ENDS_EP))
                    .header("Content-Type", "application/json")
                    .build();

            log.debug("Sending POST request to: {}", url + "/" + API_VERSION + "/blocks-ends");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.debug("Successfully pushed block ends to server. Status: {}", response.statusCode());
            } else {
                log.warn("Failed to push block ends to server. Status: {}, Response: {}", response.statusCode(), response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing block ends to server", e);
            log.error(e.fillInStackTrace().toString());
            return false;
        }
    }

    @Override
    public void closeFlushHandler() {
        // HttpClient doesn't have a close() method in standard Java 11+ implementation
        // It will be automatically cleaned up by garbage collection
    }
}