package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.serviceCall.VFLApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsynchronousBuffer implements VFLBuffer {
    private final List<LogData> logsToFlush;
    private final List<BlockData> blocksToFlush;
    private final int blockBufferSize;
    private final int logBufferSize;
    //ExecutorService are non-daemon threads by default so program does not terminate until all daemon threads are shut down
    private final ExecutorService workers;
    private final VFLApi backendAPI;
    //Multi thread variables
    private final AtomicBoolean flushInProgress = new AtomicBoolean(false);
    private final Object bufferLock = new Object();

    public AsynchronousBuffer(int blockBufferSize, int logBufferSize, int threadPoolSize, VFLApi backend) {
        logsToFlush = new ArrayList<>();
        blocksToFlush = new ArrayList<>();
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        backendAPI = backend;
        workers = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public void pushLogToBuffer(LogData log) {
        synchronized (bufferLock) {
            logsToFlush.add(log);
            flushIfFull();
        }
    }

    @Override
    public void pushBlockToBuffer(BlockData block) {
        synchronized (bufferLock) {
            blocksToFlush.add(block);
            flushIfFull();
        }
    }

    private void flushAll() {
        if (flushInProgress.compareAndSet(false, true)) {
            try {
                flushLogs();
                flushBlocks();
            } finally {
                flushInProgress.set(false);
            }
        }
    }

    // Called within synchronized block, so no additional sync needed
    private void flushIfFull() {
        if (blocksToFlush.size() >= blockBufferSize || logsToFlush.size() >= logBufferSize) {
            flushAll();
        }
    }

    private void flushLogs() {
        workers.submit(() -> {
            List<LogData> logsToProcess;

            // Extract and clear logs atomically
            synchronized (bufferLock) {
                if (logsToFlush.isEmpty()) {
                    return;
                }
                logsToProcess = new ArrayList<>(logsToFlush);
                logsToFlush.clear();
            }

            try {
                boolean success = backendAPI.pushLogsToServer(logsToProcess);
                if (!success) {
                    // Re-add failed logs to the beginning of the buffer
                    synchronized (bufferLock) {
                        logsToFlush.addAll(0, logsToProcess);
                    }
                }
            } catch (Exception e) {
                // Re-add failed logs to the beginning of the buffer
                synchronized (bufferLock) {
                    logsToFlush.addAll(0, logsToProcess);
                }
            }
        });
    }

    private void flushBlocks() {
        workers.submit(() -> {
            List<BlockData> blocksToProcess;

            synchronized (bufferLock) {
                if (blocksToFlush.isEmpty()) {
                    return;
                }
                blocksToProcess = new ArrayList<>(blocksToFlush);
                blocksToFlush.clear();
            }

            try {
                boolean success = backendAPI.pushBlocksToServer(blocksToProcess);
                if (!success) {
                    synchronized (bufferLock) {
                        blocksToFlush.addAll(0, blocksToProcess);
                    }
                }
            } catch (Exception e) {
                synchronized (bufferLock) {
                    blocksToFlush.addAll(0, blocksToProcess);
                }
                System.err.println("Failed to push blocks to server: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        workers.close();
    }
}