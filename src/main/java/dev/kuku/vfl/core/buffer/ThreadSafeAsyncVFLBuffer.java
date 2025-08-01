package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class ThreadSafeAsyncVFLBuffer implements VFLBuffer {
    private final int bufferSize;
    private final int flushTimeout;
    private final VFLFlushHandler flushHandler;
    private final List<Block> blocks = new ArrayList<>();
    private final List<Log> logs = new ArrayList<>();
    private final Map<String, Long> blockStarts = new HashMap<>();
    private final Map<String, String> blockEnds = new HashMap<>();
    private final Object lock = new Object();
    private final ExecutorService executor;

    //TODO periodic flush in future
    public ThreadSafeAsyncVFLBuffer(int bufferSize, int flushTimeoutMillisecond, VFLFlushHandler flushHandler, ExecutorService executor) {
        this.bufferSize = bufferSize;
        this.flushHandler = flushHandler;
        this.executor = executor;
        this.flushTimeout = flushTimeoutMillisecond;
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
        if (logs.isEmpty()) return;
        flushHandler.pushBlocksToServer(blocks);
    }

    private void flushLogs(List<Log> logs) {
        if (logs.isEmpty()) return;
        flushHandler.pushLogsToServer(logs);
    }

    private void flushBlockStarts(Map<String, Long> starts) {
        if (starts.isEmpty()) return;
        flushHandler.pushBlockStartsToServer(starts);
    }

    private void flushBlockEnds(Map<String, String> ends) {
        if (ends.isEmpty()) return;
        flushHandler.pushBlockEndsToServer(ends);
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
        flushIfFull();
    }

    @Override
    public void pushLogEndToBuffer(String blockId, String endMessage) {
        synchronized (lock) {
            blockEnds.put(blockId, endMessage);
        }
        flushIfFull();
    }

    @Override
    public void flushAndClose() {
        // First, flush any remaining data
        flushAll();

        // Then shutdown the executor and wait for completion
        executor.shutdown();
        try {
            //Wait for 100 ms and recheck until we reach flushTimeout
            int current = 0;
            while (current < flushTimeout) {
                current += 100;
                if (executor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    flushHandler.closeFlushHandler();
                    return;
                }
            }
            throw new TimeoutException("Waiting time for flushing exceeded configured flush timeout " + flushTimeout);
        } catch (Exception e) {
            log.error("Failed to close flush and close {}", e.fillInStackTrace(), e.getMessage());
        }
    }
}