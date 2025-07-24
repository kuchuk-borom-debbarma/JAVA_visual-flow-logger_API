package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadSafeSynchronousVflBuffer implements VFLBuffer {
    private final int logBufferSize;
    private final int blockBufferSize;
    private final VFLFlushHandler api;
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);
    private final List<Log> logsToFlush;
    private final List<Block> blocksToFlush;
    private final Object locker = new Object();

    public ThreadSafeSynchronousVflBuffer(int blockBufferSize, int logBufferSize, VFLFlushHandler api) {
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        logsToFlush = new ArrayList<>(logBufferSize);
        blocksToFlush = new ArrayList<>(blockBufferSize);
        this.api = api;
    }

    @Override
    public void pushLogToBuffer(Log log) {
        synchronized (locker) {
            logsToFlush.add(log);
        }
        //Race condition but should be fine since we do size check inside a lock in the function so if's accessed somewhere else it will wait
        flushIfFull();
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        synchronized (locker) {
            blocksToFlush.add(block);
        }
        flushIfFull();
    }

    @Override
    public void pushLogStartToBuffer(String blockId) {
        //TODO
    }

    @Override
    public void pushLogEndToBuffer(String blockId) {
        //TODO
    }


    @Override
    public void flushAndClose() {
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
                List<Log> toFlushLogs;
                List<Block> toFlushBlocks;
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
