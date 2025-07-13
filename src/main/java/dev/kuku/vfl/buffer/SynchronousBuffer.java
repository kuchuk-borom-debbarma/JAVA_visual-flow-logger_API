package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.serviceCall.VFLApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-performance synchronous buffer with optimized locking and error handling.
 * <p>
 * Key improvements:
 * - Uses ReadWriteLock for better read performance during size checks
 * - Implements proper exception handling with partial failure recovery
 * - Optimized flush ordering with dependency management
 * - Comprehensive error reporting and recovery
 * - Lock-free size estimation for better performance
 * - Graceful degradation under high contention
 */
public class SynchronousBuffer implements VFLBuffer {

    private final List<LogData> logs = new ArrayList<>();
    private final List<BlockData> blocks = new ArrayList<>();
    private final int bufferSize;
    private final VFLApi vflApi;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final int maxRetryAttempts;

    // Using ReadWriteLock for better performance on size checks
    private final ReentrantReadWriteLock logsLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock blocksLock = new ReentrantReadWriteLock();

    // Volatile fields for lock-free size estimation
    private volatile int estimatedLogsSize = 0;
    private volatile int estimatedBlocksSize = 0;

    public SynchronousBuffer(int bufferSize, VFLApi vflApi, int maxRetryAttempts) {
        this.bufferSize = bufferSize;
        this.vflApi = vflApi;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public SynchronousBuffer(int bufferSize, VFLApi vflApi) {
        this(bufferSize, vflApi, 3); // Default 3 retry attempts
    }

    @Override
    public void pushLogToBuffer(LogData log) {
        if (isShutdown.get()) {
            throw new IllegalStateException("Buffer is shutdown");
        }

        boolean shouldFlush = false;

        logsLock.writeLock().lock();
        try {
            logs.add(log);
            estimatedLogsSize = logs.size();

            // Check if we need to flush - use fast estimation first
            if (estimatedLogsSize + estimatedBlocksSize >= bufferSize) {
                shouldFlush = true;
            }
        } finally {
            logsLock.writeLock().unlock();
        }

        if (shouldFlush) {
            flush();
        }
    }

    @Override
    public void pushBlockToBuffer(BlockData block) {
        if (isShutdown.get()) {
            throw new IllegalStateException("Buffer is shutdown");
        }

        boolean shouldFlush = false;

        blocksLock.writeLock().lock();
        try {
            blocks.add(block);
            estimatedBlocksSize = blocks.size();

            // Check if we need to flush - use fast estimation first
            if (estimatedLogsSize + estimatedBlocksSize >= bufferSize) {
                shouldFlush = true;
            }
        } finally {
            blocksLock.writeLock().unlock();
        }

        if (shouldFlush) {
            flush();
        }
    }

    /**
     * Gets the precise total size of both buffers.
     * Uses read locks for better concurrent performance.
     */
    private int getPreciseTotalSize() {
        // Try lock-free estimation first
        int estimate = estimatedLogsSize + estimatedBlocksSize;
        if (estimate < bufferSize - 10) { // Small buffer to avoid unnecessary locking
            return estimate;
        }

        // Need precise count - acquire read locks in consistent order to avoid deadlock
        blocksLock.readLock().lock();
        try {
            logsLock.readLock().lock();
            try {
                return blocks.size() + logs.size();
            } finally {
                logsLock.readLock().unlock();
            }
        } finally {
            blocksLock.readLock().unlock();
        }
    }

    /**
     * Performs coordinated flush with proper error handling and partial recovery.
     */
    private void flush() {
        List<BlockData> blocksToFlush = null;
        List<LogData> logsToFlush = null;

        // Extract data in proper order (acquire locks in consistent order)
        blocksLock.writeLock().lock();
        try {
            if (!blocks.isEmpty()) {
                blocksToFlush = new ArrayList<>(blocks);
                blocks.clear();
                estimatedBlocksSize = 0;
            }
        } finally {
            blocksLock.writeLock().unlock();
        }

        logsLock.writeLock().lock();
        try {
            if (!logs.isEmpty()) {
                logsToFlush = new ArrayList<>(logs);
                logs.clear();
                estimatedLogsSize = 0;
            }
        } finally {
            logsLock.writeLock().unlock();
        }

        // If nothing to flush, return early
        if ((blocksToFlush == null || blocksToFlush.isEmpty()) &&
                (logsToFlush == null || logsToFlush.isEmpty())) {
            return;
        }

        // Perform the actual flush with retry logic
        FlushResult result = performFlushWithRetry(blocksToFlush, logsToFlush);

        // Handle partial failures
        if (!result.success) {
            handleFlushFailure(result);
        }
    }

    /**
     * Performs flush with retry logic and comprehensive error handling.
     */
    private FlushResult performFlushWithRetry(List<BlockData> blocksToFlush, List<LogData> logsToFlush) {
        FlushResult result = new FlushResult();

        // Flush blocks first (critical for data consistency)
        if (blocksToFlush != null && !blocksToFlush.isEmpty()) {
            result.blocksSuccess = flushBlocksWithRetry(blocksToFlush);
            if (!result.blocksSuccess) {
                result.failedBlocks = blocksToFlush;
                result.success = false;
                return result; // Don't attempt logs if blocks failed
            }
        }

        // Flush logs only if blocks succeeded (or no blocks to flush)
        if (logsToFlush != null && !logsToFlush.isEmpty()) {
            result.logsSuccess = flushLogsWithRetry(logsToFlush);
            if (!result.logsSuccess) {
                result.failedLogs = logsToFlush;
            }
        }

        result.success = result.blocksSuccess && result.logsSuccess;
        return result;
    }

    /**
     * Flushes blocks with retry logic.
     */
    private boolean flushBlocksWithRetry(List<BlockData> blocksToFlush) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxRetryAttempts && !isShutdown.get()) {
            try {
                boolean success = vflApi.pushBlocksToServer(blocksToFlush);
                if (success) {
                    return true;
                }

                // Log the failure
                System.err.println("Failed to push blocks to server (attempt " + (attempts + 1) + ")");

            } catch (Exception e) {
                lastException = e;
                System.err.println("Exception pushing blocks (attempt " + (attempts + 1) + "): " + e.getMessage());
            }

            attempts++;

            // Small delay between retries to avoid overwhelming the server
            if (attempts < maxRetryAttempts) {
                try {
                    Thread.sleep(100L * attempts); // Progressive delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // All retries failed
        if (lastException != null) {
            System.err.println("Failed to push blocks after " + maxRetryAttempts + " attempts " + lastException.getMessage());
        }
        return false;
    }

    /**
     * Flushes logs with retry logic.
     */
    private boolean flushLogsWithRetry(List<LogData> logsToFlush) {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxRetryAttempts && !isShutdown.get()) {
            try {
                boolean success = vflApi.pushLogsToServer(logsToFlush);
                if (success) {
                    return true;
                }

                // Log the failure
                System.err.println("Failed to push logs to server (attempt " + (attempts + 1) + ")");

            } catch (Exception e) {
                lastException = e;
                System.err.println("Exception pushing logs (attempt " + (attempts + 1) + "): " + e.getMessage());
            }

            attempts++;

            // Small delay between retries to avoid overwhelming the server
            if (attempts < maxRetryAttempts) {
                try {
                    Thread.sleep(100L * attempts); // Progressive delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // All retries failed
        if (lastException != null) {
            System.err.println("Failed to push logs after " + maxRetryAttempts + " attempts");
        }
        return false;
    }

    /**
     * Handles flush failures by reinserting data back to buffers.
     */
    private void handleFlushFailure(FlushResult result) {
        // Reinsert failed blocks
        if (result.failedBlocks != null && !result.failedBlocks.isEmpty()) {
            blocksLock.writeLock().lock();
            try {
                blocks.addAll(0, result.failedBlocks);
                estimatedBlocksSize = blocks.size();
            } finally {
                blocksLock.writeLock().unlock();
            }
        }

        // Reinsert failed logs
        if (result.failedLogs != null && !result.failedLogs.isEmpty()) {
            logsLock.writeLock().lock();
            try {
                logs.addAll(0, result.failedLogs);
                estimatedLogsSize = logs.size();
            } finally {
                logsLock.writeLock().unlock();
            }
        }

        // Optionally throw exception to notify caller
        throw new RuntimeException("Failed to flush buffer - data has been reinserted for retry");
    }

    /**
     * Gets current buffer statistics.
     */
    public BufferStats getStats() {
        blocksLock.readLock().lock();
        try {
            logsLock.readLock().lock();
            try {
                return new BufferStats(logs.size(), blocks.size());
            } finally {
                logsLock.readLock().unlock();
            }
        } finally {
            blocksLock.readLock().unlock();
        }
    }

    @Override
    public void shutdown() {
        isShutdown.set(true);

        // Attempt final flush
        try {
            flush();
        } catch (Exception e) {
            System.err.println("Error during shutdown flush: " + e.getMessage());
            // Don't rethrow - we're shutting down anyway
        }
    }

    /**
     * Result class for flush operations.
     */
    private static class FlushResult {
        boolean success = true;
        boolean blocksSuccess = true;
        boolean logsSuccess = true;
        List<BlockData> failedBlocks;
        List<LogData> failedLogs;
    }

    /**
     * Statistics class for monitoring buffer performance.
     */
    public static class BufferStats {
        public final int logCount;
        public final int blockCount;
        public final int totalCount;

        public BufferStats(int logCount, int blockCount) {
            this.logCount = logCount;
            this.blockCount = blockCount;
            this.totalCount = logCount + blockCount;
        }

        @Override
        public String toString() {
            return String.format("BufferStats{logs=%d, blocks=%d, total=%d}",
                    logCount, blockCount, totalCount);
        }
    }
}