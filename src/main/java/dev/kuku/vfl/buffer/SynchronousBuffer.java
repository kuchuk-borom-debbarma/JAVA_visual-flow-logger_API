package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.VflResponse;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.util.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class SynchronousBuffer implements VFLBuffer {
    final List<LogData> logs;
    final List<BlockData> blocks;
    private final int bufferSize;
    private final ApiClient apiClient;

    public SynchronousBuffer(int bufferSize) {
        logs = new ArrayList<>();
        blocks = new ArrayList<>();
        this.bufferSize = bufferSize;
        this.apiClient = new ApiClient();
    }

    @Override
    public void pushLogToBuffer(LogData log) {
        logs.add(log);
        flushIfRequired();
    }

    @Override
    public void pushBlockToBuffer(BlockData block) {
        blocks.add(block);
        flushIfRequired();
    }

    private void flushIfRequired() {
        if (blocks.size() + logs.size() < bufferSize) {
            return;
        }
        flush();
    }

    private void flush() {
        List<LogData> logsToFlush;
        List<BlockData> blocksToFlush;
        synchronized (this.blocks) {
            synchronized (this.logs) {
                logsToFlush = new ArrayList<>(this.logs);
                blocksToFlush = new ArrayList<>(this.blocks);
                this.logs.clear();
            }
            this.blocks.clear();
        }
        VflResponse<Boolean> response;
        String host = "http://localhost:8080/api/v1";
        try {
            response = apiClient.post(String.format("%s/block/", host), blocksToFlush, Boolean.class);
            System.out.printf("response for saving block = %s%n", response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            response = apiClient.post(String.format("%s/vfl/logs", host), logsToFlush, Boolean.class);
            System.out.printf("response for saving logs : %s%n", response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void flushAll() {
        flush();
    }
}
