package dev.kuku.vfl.core.buffer.flushHandler;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.VflResponse;
import dev.kuku.vfl.core.util.ApiClient;

import java.util.List;

/**
 * Meant to be used for communicating with simple VFL_Server.
 */
public class NaiveServerFlushHandler implements VFLFlushHandler {
    private final String url;
    private final ApiClient apiClient;

    public NaiveServerFlushHandler(String url) {
        this.url = String.format("%s/api/v1", url);
        this.apiClient = new ApiClient();
    }

    @Override
    public boolean pushLogsToServer(List<Log> logs) {
        VflResponse<Boolean> response;
        String path = this.url + "/vfl/logs";
        try {
            response = apiClient.post(path, logs, Boolean.class);
            return response.getData();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean pushBlocksToServer(List<Block> blocks) {
        VflResponse<Boolean> response;
        String path = this.url + "/block/";
        try {
            response = apiClient.post(path, blocks, Boolean.class);
            return response.getData();
        } catch (Exception e) {
            return false;
        }
    }
}
