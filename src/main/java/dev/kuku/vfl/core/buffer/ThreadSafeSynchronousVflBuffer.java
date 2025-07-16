package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.serviceCall.VFLApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadSafeSynchronousVflBuffer implements VFLBuffer {
    private final int logBufferSize;
    private final int blockBufferSize;
    private final VFLApi api;
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);
    private final List<LogData> logsToFlush;
    private final List<BlockData> blocksToFlush;
    private final Object locker = new Object();

    public ThreadSafeSynchronousVflBuffer(int blockBufferSize, int logBufferSize, VFLApi api) {
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        logsToFlush = new ArrayList<>(logBufferSize);
        blocksToFlush = new ArrayList<>(blockBufferSize);
        this.api = api;
    }

    @Override
    public void pushLogToBuffer(LogData log) {
        synchronized (locker) {
            logsToFlush.add(log);
        }
        //Race condition but should be fine since we do size check inside a lock in the function
        flushIfFull();
    }

    @Override
    public void pushBlockToBuffer(BlockData block) {
        synchronized (locker) {
            blocksToFlush.add(block);
        }
        flushIfFull();
    }


    @Override
    public void shutdown() {
        flush();
    }

    private void flushIfFull() {
        boolean shouldFlush = false;
        synchronized (locker) {
            if (logsToFlush.size() > logBufferSize || blocksToFlush.size() > blockBufferSize) {
                shouldFlush = true;
            }
        }
        //Race condition should be okay here since we lock inside flush again and do another check
        if (shouldFlush) {
            flush();
        }
    }

    private void flush() {
        if (isFlushing.compareAndSet(false, true)) {
            try {
                List<LogData> toFlushLogs;
                List<BlockData> toFlushBlocks;
                synchronized (locker) {
                    toFlushLogs = new ArrayList<>(logsToFlush);
                    toFlushBlocks = new ArrayList<>(blocksToFlush);
                    blocksToFlush.clear();
                    logsToFlush.clear();
                }
                if (!toFlushBlocks.isEmpty()) {
                    api.pushBlocksToServer(toFlushBlocks);
                }
                if (!toFlushLogs.isEmpty()) {
                    api.pushLogsToServer(toFlushLogs);
                }
            } finally {
                isFlushing.set(false);
            }
        }
    }
}
