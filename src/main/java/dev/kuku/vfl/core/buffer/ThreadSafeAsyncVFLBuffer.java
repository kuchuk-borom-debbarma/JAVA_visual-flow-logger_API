package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadSafeAsyncVFLBuffer implements VFLBuffer {
    private final int bufferSize;
    private final VFLFlushHandler flushHandler;
    private final List<Block> blocks = new ArrayList<>();
    private final List<Log> logs = new ArrayList<>();
    private final Map<String, Long> blockStarts = new HashMap<>(); //TODO make block start take timestamp
    private final Map<String, String> blockEnds = new HashMap<>();
    private final Object lock = new Object();
    private final ExecutorService executor;

    public ThreadSafeAsyncVFLBuffer(int bufferSize, VFLFlushHandler flushHandler, ExecutorService executor) {
        this.bufferSize = bufferSize;
        this.flushHandler = flushHandler;
        this.executor = executor;
    }

    private void flushIfFull() {
        boolean shouldFlush = false;
        synchronized (lock) {
            int currentSize = blocks.size() + logs.size() + blockStarts.size() + blockEnds.size();
            if (currentSize > bufferSize) {
                shouldFlush = true;
            }
        }
        if (shouldFlush) {
            flushAll();
        }
    }

    // Multiple flush should be fine as we are locking and then copying the data and then clearing. The lists can't be accessed during that time
    private void flushAll() {
        List<Block> blocksToFlush;
        List<Log> logsToFlush;
        Map<String, Long> blockStarts2Flush;
        Map<String, String> blockEnds2Flush;
        synchronized (lock) {
            blocksToFlush = new ArrayList<>(blocks);
            logsToFlush = new ArrayList<>(logs);
            blockStarts2Flush = new HashMap<>(blockStarts);
            blockEnds2Flush = new HashMap<>(blockEnds);
            executor.submit(() -> {
                flushBlocks(blocksToFlush);
                flushLogs(logsToFlush);
                flushBlockStarts(blockStarts2Flush);
                flushBlockEnds(blockEnds2Flush);
            });
            blocks.clear();
            logs.clear();
            blockStarts.clear();
            blockEnds.clear();
        }
    }

    private void flushBlocks(List<Block> blocks) {

    }

    private void flushLogs(List<Log> logs) {

    }

    private void flushBlockStarts(Map<String, Long> starts) {

    }

    private void flushBlockEnds(Map<String, String> ends) {

    }

    @Override
    public void pushLogToBuffer(Log log) {
        synchronized (lock) {
            logs.add(log);
        }
        flushIfFull();
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        synchronized (lock) {
            blocks.add(block);
        }
        flushIfFull();
    }

    @Override
    public void pushLogStartToBuffer(String blockId, long timestamp) {
        synchronized (lock) {
            blockStarts.put(blockId, timestamp);
        }
    }

    @Override
    public void pushLogEndToBuffer(String blockId, String endMessage) {
        synchronized (lock) {
            blockEnds.put(blockId, endMessage);
        }
    }

    @Override
    public void flushAndClose() {
        try {
            executor.awaitTermination(999, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
