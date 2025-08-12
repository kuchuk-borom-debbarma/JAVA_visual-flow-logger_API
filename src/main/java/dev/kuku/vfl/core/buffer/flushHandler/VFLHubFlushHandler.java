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
 * VFL flush handler that sends batched logs and blocks to a VFL Hub via blocking HTTP calls.
 *
 * <p>This implementation uses the Java 11+ {@link HttpClient} in blocking mode to ensure requests
 * are sent in order. This blocking behavior is suitable when order matters for the log data.
 * When used with an asynchronous buffer like {@link dev.kuku.vfl.core.buffer.AsyncBuffer},
 * the blocking calls run in a separate executor thread and do not block the main application.
 *
 * <p><b>Features:</b>
 * <ul>
 *   <li>Serializes data to JSON using Jackson {@link ObjectMapper}</li>
 *   <li>Performs POST requests to configured endpoints for logs, blocks, block starts, and block ends</li>
 *   <li>Logs detailed debug and warning information on each request and response</li>
 *   <li>Retries and error handling are left to the caller or upper layers</li>
 * </ul>
 *
 * <p><b>Usage:</b> Instantiate with the base URI of your VFL Hub endpoint (e.g., "<a href="http://host:port">http://localhost:8080</a>").
 * This handler can then be passed to a {@link dev.kuku.vfl.core.buffer.VFLBuffer} implementation
 * that supports flushing.</p>
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
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        return postData(logs, ADD_LOGS_EP, "logs");
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        return postData(blocks, ADD_BLOCKS_EP, "blocks");
    }

    @Override
    public boolean pushBlockStartsToServer(Map<String, Long> blockStarts) {
        return postData(blockStarts, ADD_BLOCK_STARTS_EP, "block starts");
    }

    @Override
    public boolean pushBlockEndsToServer(Map<String, BlockEndData> blockEnds) {
        return postData(blockEnds, ADD_BLOCK_ENDS_EP, "block ends");
    }

    private <T> boolean postData(T data, String endpoint, String dataDescription) {
        log.debug("Attempting to push {} {} to server", (data instanceof List<?> ? ((List<?>) data).size() : ((Map<?, ?>) data).size()), dataDescription);
        try {
            String jsonBody = objectMapper.writeValueAsString(data);
            log.trace("Serialized {} to JSON: {} characters", dataDescription, jsonBody.length());

            String fullUrl = url.toString() + "/" + API_VERSION + endpoint;
            HttpRequest request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .build();

            log.debug("Sending POST request to: {}", fullUrl);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            if (success) {
                log.debug("Successfully pushed {} to server. Status: {}", dataDescription, response.statusCode());
            } else {
                log.warn("Failed to push {} to server. Status: {}, Response: {}", dataDescription, response.statusCode(), response.body());
            }

            return success;
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing {} to server", dataDescription, e);
            return false;
        }
    }

    @Override
    public void closeFlushHandler() {
        // The java.net.http.HttpClient does not require explicit shutdown.
        // Resources will be cleaned up automatically.
    }
}
