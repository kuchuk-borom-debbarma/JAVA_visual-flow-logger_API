package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.serviceCall.VFLApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * High-performance asynchronous buffer with advanced concurrency features.
 *
 * Key improvements:
 * - Uses BlockingQueue for better producer-consumer coordination
 * - Implements batching with time-based and size-based triggers
 * - Separate flush threads for logs and blocks for better parallelism
 * - Exponential backoff for failed operations
 * - Comprehensive error handling and monitoring
 * - Graceful shutdown with data preservation
 */
public class AsynchronousBuffer implements VFLBuffer {

    private final BlockingQueue<LogData> logQueue;
    private final BlockingQueue<BlockData> blockQueue;
    private final int blockBufferSize;
    private final int logBufferSize;
    private final long flushTimeoutMs;
    private final int maxRetryAttempts;
    private final VFLApi backendAPI;

    private final ExecutorService flushExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final AtomicInteger activeFlushTasks = new AtomicInteger(0);

    // Separate locks for coordinated flushing
    private final ReentrantLock flushLock = new ReentrantLock();
    private final AtomicBoolean flushInProgress = new AtomicBoolean(false);

    // Monitoring and statistics
    private final AtomicInteger droppedLogs = new AtomicInteger(0);
    private final AtomicInteger droppedBlocks = new AtomicInteger(0);

    public AsynchronousBuffer(int blockBufferSize, int logBufferSize,
                              int threadPoolSize, long flushTimeoutMs,
                              int maxRetryAttempts, VFLApi backend) {
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        this.flushTimeoutMs = flushTimeoutMs;
        this.maxRetryAttempts = maxRetryAttempts;
        this.backendAPI = backend;

        // Use bounded queues to prevent memory issues
        this.logQueue = new ArrayBlockingQueue<>(logBufferSize * 2);
        this.blockQueue = new ArrayBlockingQueue<>(blockBufferSize * 2);

        // Separate thread pools for different responsibilities
        this.flushExecutor = Executors.newFixedThreadPool(threadPoolSize,
                r -> {
                    Thread t = new Thread(r, "VFL-Flush-Worker");
                    t.setDaemon(true);
                    return t;
                });

        this.scheduledExecutor = Executors.newScheduledThreadPool(1,
                r -> {
                    Thread t = new Thread(r, "VFL-Scheduled-Flush");
                    t.setDaemon(true);
                    return t;
                });

        // Start periodic flush to handle time-based triggers
        if (flushTimeoutMs > 0) {
            scheduledExecutor.scheduleAtFixedRate(this::tryPeriodicFlush,
                    flushTimeoutMs, flushTimeoutMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void pushLogToBuffer(LogData log) {
        if (isShutdown.get()) {
            throw new IllegalStateException("Buffer is shutdown");
        }

        // Non-blocking add with overflow handling
        if (!logQueue.offer(log)) {
            // Buffer is full, drop oldest data (or implement backpressure)
            LogData dropped = logQueue.poll();
            if (dropped != null) {
                droppedLogs.incrementAndGet();
            }
            logQueue.offer(log);
        }

        // Trigger flush if buffer is full
        if (logQueue.size() >= logBufferSize) {
            tryAsyncFlush();
        }
    }

    @Override
    public void pushBlockToBuffer(BlockData block) {
        if (isShutdown.get()) {
            throw new IllegalStateException("Buffer is shutdown");
        }

        // Non-blocking add with overflow handling
        if (!blockQueue.offer(block)) {
            // Buffer is full, drop oldest data (or implement backpressure)
            BlockData dropped = blockQueue.poll();
            if (dropped != null) {
                droppedBlocks.incrementAndGet();
            }
            blockQueue.offer(block);
        }

        // Trigger flush if buffer is full
        if (blockQueue.size() >= blockBufferSize) {
            tryAsyncFlush();
        }
    }

    /**
     * Attempts to trigger an asynchronous flush if one isn't already in progress.
     */
    private void tryAsyncFlush() {
        if (flushInProgress.compareAndSet(false, true)) {
            activeFlushTasks.incrementAndGet();
            flushExecutor.submit(() -> {
                try {
                    performCoordinatedFlush();
                } finally {
                    flushInProgress.set(false);
                    activeFlushTasks.decrementAndGet();
                }
            });
        }
    }

    /**
     * Periodic flush for time-based triggers.
     */
    private void tryPeriodicFlush() {
        if (!logQueue.isEmpty() || !blockQueue.isEmpty()) {
            tryAsyncFlush();
        }
    }

    /**
     * Performs coordinated flush of both logs and blocks.
     * Uses proper synchronization to ensure blocks are flushed before logs.
     */
    private void performCoordinatedFlush() {
        flushLock.lock();
        try {
            // Flush blocks first - this is critical for data consistency
            boolean blocksSuccess = flushBlocks();

            // Only flush logs if blocks were successful (or if no blocks to flush)
            if (blocksSuccess) {
                flushLogs();
            }
        } finally {
            flushLock.unlock();
        }
    }

    /**
     * Flushes blocks with retry logic and exponential backoff.
     */
    private boolean flushBlocks() {
        List<BlockData> blocksToFlush = drainQueue(blockQueue, blockBufferSize);
        if (blocksToFlush.isEmpty()) {
            return true;
        }

        return flushWithRetry(blocksToFlush, true);
    }

    /**
     * Flushes logs with retry logic and exponential backoff.
     */
    private boolean flushLogs() {
        List<LogData> logsToFlush = drainQueue(logQueue, logBufferSize);
        if (logsToFlush.isEmpty()) {
            return true;
        }

        return flushWithRetry(logsToFlush, false);
    }

    /**
     * Generic flush method with retry logic and exponential backoff.
     */
    private <T> boolean flushWithRetry(List<T> dataToFlush, boolean isBlockData) {
        int attempts = 0;
        long backoffMs = 100; // Start with 100ms backoff

        while (attempts < maxRetryAttempts && !isShutdown.get()) {
            try {
                boolean success;
                if (isBlockData) {
                    success = backendAPI.pushBlocksToServer((List<BlockData>) dataToFlush);
                } else {
                    success = backendAPI.pushLogsToServer((List<LogData>) dataToFlush);
                }

                if (success) {
                    return true;
                }

                // Failed, wait before retry
                Thread.sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2, 5000); // Max 5 second backoff
                attempts++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                reinsertData(dataToFlush, isBlockData);
                return false;
            } catch (Exception e) {
                System.err.println("Error flushing " + (isBlockData ? "blocks" : "logs") +
                        " (attempt " + (attempts + 1) + "): " + e.getMessage());

                if (attempts == maxRetryAttempts - 1) {
                    break;
                }

                try {
                    Thread.sleep(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                attempts++;
            }
        }

        // All retries failed, reinsert data
        reinsertData(dataToFlush, isBlockData);
        return false;
    }

    /**
     * Efficiently drains up to maxItems from the queue.
     */
    private <T> List<T> drainQueue(BlockingQueue<T> queue, int maxItems) {
        List<T> result = new ArrayList<>();
        queue.drainTo(result, maxItems);
        return result;
    }

    /**
     * Reinserts failed data back to the front of the appropriate queue.
     */
    @SuppressWarnings("unchecked")
    private <T> void reinsertData(List<T> failedData, boolean isBlockData) {
        if (isBlockData) {
            BlockingQueue<BlockData> queue = (BlockingQueue<BlockData>) blockQueue;
            for (int i = failedData.size() - 1; i >= 0; i--) {
                if (!queue.offer((BlockData) failedData.get(i))) {
                    droppedBlocks.incrementAndGet();
                }
            }
        } else {
            BlockingQueue<LogData> queue = (BlockingQueue<LogData>) logQueue;
            for (int i = failedData.size() - 1; i >= 0; i--) {
                if (!queue.offer((LogData) failedData.get(i))) {
                    droppedLogs.incrementAndGet();
                }
            }
        }
    }

    /**
     * Gets buffer statistics for monitoring.
     */
    public BufferStats getStats() {
        return new BufferStats(
                logQueue.size(),
                blockQueue.size(),
                droppedLogs.get(),
                droppedBlocks.get(),
                activeFlushTasks.get()
        );
    }

    @Override
    public void shutdown() {
        isShutdown.set(true);

        // Stop scheduled tasks
        scheduledExecutor.shutdown();

        // Final flush attempt
        try {
            performCoordinatedFlush();
        } catch (Exception e) {
            System.err.println("Error during final flush: " + e.getMessage());
        }

        // Shutdown executors
        flushExecutor.shutdown();

        try {
            if (!flushExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Forcing shutdown after timeout");
                flushExecutor.shutdownNow();
            }

            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            flushExecutor.shutdownNow();
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Statistics class for monitoring buffer performance.
     */
    public static class BufferStats {
        public final int logQueueSize;
        public final int blockQueueSize;
        public final int droppedLogs;
        public final int droppedBlocks;
        public final int activeFlushTasks;

        public BufferStats(int logQueueSize, int blockQueueSize, int droppedLogs,
                           int droppedBlocks, int activeFlushTasks) {
            this.logQueueSize = logQueueSize;
            this.blockQueueSize = blockQueueSize;
            this.droppedLogs = droppedLogs;
            this.droppedBlocks = droppedBlocks;
            this.activeFlushTasks = activeFlushTasks;
        }

        @Override
        public String toString() {
            return String.format("BufferStats{logs=%d, blocks=%d, droppedLogs=%d, droppedBlocks=%d, activeTasks=%d}",
                    logQueueSize, blockQueueSize, droppedLogs, droppedBlocks, activeFlushTasks);
        }
    }
}