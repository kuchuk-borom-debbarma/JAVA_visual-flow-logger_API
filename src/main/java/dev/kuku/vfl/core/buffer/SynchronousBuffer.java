package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.serviceCall.VFLApi;

import java.util.ArrayList;
import java.util.List;

//TODO use backend service interface to make api calls
public class SynchronousBuffer implements VFLBuffer {
    final List<LogData> logs;
    final List<BlockData> blocks;
    private final int bufferSize;
    private final VFLApi vflApi;

    public SynchronousBuffer(int bufferSize, VFLApi vflApi) {
        logs = new ArrayList<>();
        blocks = new ArrayList<>();
        this.bufferSize = bufferSize;
        this.vflApi = vflApi;
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
        try {
            vflApi.pushBlocksToServer(blocksToFlush);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            vflApi.pushLogsToServer(logsToFlush);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        flush();
    }
}
