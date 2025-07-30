package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadSafeSynchronousVflBuffer implements VFLBuffer {
    private final int bufferSize;
    private final VFLFlushHandler api;
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);
    private final List<Log> logsToFlush;
    private final List<Block> blocksToFlush;
    private final Map<String, Long> blockStarts;
    private final Map<String, String> blockEnds;
    private final Object locker = new Object();

    public ThreadSafeSynchronousVflBuffer(int bufferSize, VFLFlushHandler api) {
        this.bufferSize = bufferSize;
        logsToFlush = new ArrayList<>();
        blocksToFlush = new ArrayList<>();
        blockStarts = new HashMap<>();
        blockEnds = new HashMap<>();
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
    public void pushLogStartToBuffer(String blockId, long timestamp) {
        synchronized (locker) {
            blockStarts.put(blockId, timestamp);
        }
        flushIfFull();
    }

    @Override
    public void pushLogEndToBuffer(String blockId, String endMessage) {
        synchronized (locker) {
            blockEnds.put(blockId, endMessage);
        }
        flushIfFull();
    }


    @Override
    public void flushAndClose() {
        flush();
    }

    private void flushIfFull() {
        boolean shouldFlush = false;
        synchronized (locker) {
            int size = logsToFlush.size() + blocksToFlush.size() + blockStarts.size() + blockEnds.size();
            if (size > bufferSize) {
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
                Map<String, Long> toFlushBlockStarts;
                Map<String, String> toFLushBLockENds;
                synchronized (locker) {
                    toFlushLogs = new ArrayList<>(logsToFlush);
                    toFlushBlocks = new ArrayList<>(blocksToFlush);
                    toFlushBlockStarts = new HashMap<>(blockStarts);
                    toFLushBLockENds = new HashMap<>(blockEnds);
                    blocksToFlush.clear();
                    logsToFlush.clear();
                    blockStarts.clear();
                    blockEnds.clear();
                }
                if (!toFlushBlocks.isEmpty()) {
                    api.pushBlocksToServer(toFlushBlocks);
                }
                if (!toFlushLogs.isEmpty()) {
                    api.pushLogsToServer(toFlushLogs);
                }
                if (!toFlushBlockStarts.isEmpty()) {
                    api.pushBlockStartsToServer(toFlushBlockStarts);
                }
                if (!toFLushBLockENds.isEmpty()) {
                    api.pushBlockEndsToServer(toFLushBLockENds);
                }
            } finally {
                isFlushing.set(false);
            }
        }
    }
}
