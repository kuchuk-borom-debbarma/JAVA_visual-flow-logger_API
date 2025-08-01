package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import org.javatuples.Pair;

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
    private final Map<String, Pair<Long, String>> blockEnds2Flush;

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
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        lock.lock();
        try {
            this.blocks2Flush.add(block);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void pushLogStartToBuffer(String blockId, long timestamp) {
        lock.lock();
        try {
            this.blockStarts2Flush.put(blockId, timestamp);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void pushLogEndToBuffer(String blockId, Pair<Long, String> endTimeAndMessage) {
        lock.lock();
        try {
            blockEnds2Flush.put(blockId, endTimeAndMessage);
        } finally {
            lock.unlock();
        }
    }

    private void flushIfFull() {
        lock.lock();x
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

    private void flushAll() {
        List<Log> l;
        List<Block> b;
        Map<String, Long> bs;
        Map<String, Pair<Long, String>> be;
        lock.lock();
        try {
            l = new ArrayList<>(logs2Flush);
            b = new ArrayList<>(blocks2Flush);
            bs = new HashMap<>(blockStarts2Flush);
            be = new HashMap<>(blockEnds2Flush);
            logs2Flush.clear();
            blocks2Flush.clear();
            blockStarts2Flush.clear();
            blockEnds2Flush.clear();
        } finally {
            lock.unlock();
        }
        if (!l.isEmpty() || !b.isEmpty() || !bs.isEmpty() || !be.isEmpty()) {
            flushBlocks(b);
            flushBlockStarts(bs);
            flushBlockEnds(be);
            flushLogs(l);
        }
    }

    @Override
    public void flushAndClose() {
        flushAll();
    }

    protected abstract void flushBlockEnds(Map<String, Pair<Long, String>> blockEnds);

    protected abstract void flushBlocks(List<Block> blocks);

    protected abstract void flushBlockStarts(Map<String, Long> blockStarts);

    protected abstract void flushLogs(List<Log> logs);
}
