package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.serviceCall.VFLApi;

import java.util.ArrayList;
import java.util.List;

public class SynchronousBuffer implements VFLBuffer {
    private final List<LogData> logs;
    private final List<BlockData> blocks;
    private final int bufferSize;
    private final VFLApi vflApi;
    private volatile boolean isShutdown = false;

    public SynchronousBuffer(int bufferSize, VFLApi vflApi) {
        this.logs = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.bufferSize = bufferSize;
        this.vflApi = vflApi;
    }

    @Override
    public synchronized void pushLogToBuffer(LogData log) {
        if (isShutdown) {
            throw new IllegalStateException("Buffer is shutdown");
        }

        logs.add(log);
        flushIfRequired();
    }

    @Override
    public synchronized void pushBlockToBuffer(BlockData block) {
        if (isShutdown) {
            throw new IllegalStateException("Buffer is shutdown");
        }

        blocks.add(block);
        flushIfRequired();
    }

    private synchronized void flushIfRequired() {
        if (blocks.size() + logs.size() >= bufferSize) {
            flush();
        }
    }

    private synchronized void flush() {
        if (logs.isEmpty() && blocks.isEmpty()) {
            return;
        }

        List<LogData> logsToFlush = new ArrayList<>(logs);
        List<BlockData> blocksToFlush = new ArrayList<>(blocks);

        logs.clear();
        blocks.clear();

        // Make API calls while holding the lock
        // This is simple but may block other threads longer
        try {
            if (!blocksToFlush.isEmpty()) {
                vflApi.pushBlocksToServer(blocksToFlush);
            }
            if (!logsToFlush.isEmpty()) {
                vflApi.pushLogsToServer(logsToFlush);
            }
        } catch (Exception e) {
            blocks.addAll(0, blocksToFlush);
            logs.addAll(0, logsToFlush);
            throw new RuntimeException("Failed to flush buffer", e);
        }
    }

    @Override
    public synchronized void shutdown() {
        isShutdown = true;
        flush();
    }
}