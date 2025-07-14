package dev.kuku.vfl.core.serviceCall;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflResponse;
import dev.kuku.vfl.util.ApiClient;

import java.util.List;

/**
 * Meant to be used for communicating with simple VFL_Server.
 */
public class NaiveVFLServerAPI implements VFLApi {
    private final String url;
    private final ApiClient apiClient;

    public NaiveVFLServerAPI(String url) {
        this.url = String.format("%s/api/v1", url);
        this.apiClient = new ApiClient();
    }

    @Override
    public boolean pushLogsToServer(List<LogData> logs) {
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
    public boolean pushBlocksToServer(List<BlockData> blocks) {
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
