package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsynchronousBuffer implements VFLBuffer {
    private final List<LogData> logsToFlush;
    private final List<BlockData> blocksToFlush;
    private final int blockBufferSize;
    private final int logBufferSize;
    //ExecutorService are non-daemon threads by default, so program does not terminate until all daemon threads are shut down
    private final ExecutorService workers;
    private final VFLFlushHandler backendAPI;

    public AsynchronousBuffer(int blockBufferSize, int logBufferSize, int threadPoolSize, VFLFlushHandler backend) {
        logsToFlush = new ArrayList<>();
        blocksToFlush = new ArrayList<>();
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        backendAPI = backend;
        workers = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public synchronized void pushLogToBuffer(LogData log) {
        logsToFlush.add(log);
        flushIfFull();
    }

    @Override
    public synchronized void pushBlockToBuffer(BlockData block) {
        blocksToFlush.add(block);
        flushIfFull();
    }

    // Called within synchronized block, so no additional sync needed
    private void flushIfFull() {
        boolean shouldFlush = false;
        synchronized (blocksToFlush) {
            synchronized (logsToFlush) {
                if (blocksToFlush.size() >= blockBufferSize || logsToFlush.size() >= logBufferSize) {
                    shouldFlush = true;
                }
            }
        }
        if (shouldFlush) {
            flushAll();
        }
    }
    //TODO needs to be run  in async manner. First block and then log instead of two separate operations
    //TODO single flush operation at once for less complexity. Use atomic bool for this
    private void flushAll() {
        flushBlocks();
        flushLogs();
    }

    private void flushBlocks() {
        workers.submit(() -> {
            List<BlockData> blocksToProcess;

            synchronized (blocksToFlush) {
                if (blocksToFlush.isEmpty()) {
                    return;
                }
                blocksToProcess = new ArrayList<>(blocksToFlush);
                blocksToFlush.clear();
            }

            try {
                boolean success = backendAPI.pushBlocksToServer(blocksToProcess);
                if (!success) {
                    synchronized (blocksToFlush) {
                        blocksToFlush.addAll(0, blocksToProcess);
                    }
                }
            } catch (Exception e) {
                synchronized (blocksToFlush) {
                    blocksToFlush.addAll(0, blocksToProcess);
                }
                System.err.println("Failed to push blocks to server: " + e.getMessage());
            }
        });
    }


    private void flushLogs() {
        workers.submit(() -> {
            List<LogData> logsToProcess;

            // Extract and clear logs atomically
            synchronized (logsToFlush) {
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
                    synchronized (logsToFlush) {
                        logsToFlush.addAll(0, logsToProcess);
                    }
                }
            } catch (Exception e) {
                // Re-add failed logs to the beginning of the buffer
                synchronized (logsToFlush) {
                    logsToFlush.addAll(0, logsToProcess);
                }
            }
        });
    }

    @Override
    public void flushAndClose() {
        workers.close();
    }
}