package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public abstract class VFLBufferBase implements VFLBuffer {
    private final int bufferSize;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Log> logs2Flush;
    private final List<Block> blocks2Flush;
    private final Map<String, Long> blockStarts2Flush;
    private final Map<String, BlockEndData> blockEnds2Flush;

    public VFLBufferBase(int bufferSize) {
        this.bufferSize = bufferSize;
        logs2Flush = new ArrayList<>();
        blocks2Flush = new ArrayList<>();
        blockStarts2Flush = new HashMap<>();
        blockEnds2Flush = new HashMap<>();
    }

    @Override
    public void pushLogToBuffer(Log log) {
        lock.lock();
        try {
            this.logs2Flush.add(log);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        lock.lock();
        try {
            this.blocks2Flush.add(block);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    @Override
    public void pushLogStartToBuffer(String blockId, long timestamp) {
        lock.lock();
        try {
            this.blockStarts2Flush.put(blockId, timestamp);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    @Override
    public void pushLogEndToBuffer(String blockId, BlockEndData endTimeAndMessage) {
        lock.lock();
        try {
            blockEnds2Flush.put(blockId, endTimeAndMessage);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    private void flushIfFull() {
        lock.lock();
        boolean shouldFlush = false;
        try {
            int logsSize = logs2Flush.size();
            int blocksSize = blocks2Flush.size();
            int blockStartsSize = blockStarts2Flush.size();
            int blockEndsSize = blockEnds2Flush.size();
            int totalSize = logsSize + blocksSize + blockStartsSize + blockEndsSize;
            if (totalSize > bufferSize) {
                shouldFlush = true;
            }
        } finally {
            lock.unlock();
        }
        if (shouldFlush) {
            flushAll();
        }
    }

    protected void flushAll() {
        List<Log> logsToFlush;
        List<Block> blocksToFlush;
        Map<String, Long> blockStartsToFlush;
        Map<String, BlockEndData> blockEndsToFlush;

        // Minimize lock time - only hold lock while copying and clearing data
        lock.lock();
        try {
            logsToFlush = new ArrayList<>(logs2Flush);
            blocksToFlush = new ArrayList<>(blocks2Flush);
            blockStartsToFlush = new HashMap<>(blockStarts2Flush);
            blockEndsToFlush = new HashMap<>(blockEnds2Flush);
            logs2Flush.clear();
            blocks2Flush.clear();
            blockStarts2Flush.clear();
            blockEnds2Flush.clear();
        } finally {
            lock.unlock(); // Release lock BEFORE calling onFlushAll
        }

        // multiple threads can execute onFlushAll concurrently
        // since each has their own copy of the data
        onFlushAll(logsToFlush, blocksToFlush, blockStartsToFlush, blockEndsToFlush);
    }

    @Override
    public void flushAndClose() {
        flushAll();
    }

    protected abstract void onFlushAll(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, BlockEndData> blockEnds);
}