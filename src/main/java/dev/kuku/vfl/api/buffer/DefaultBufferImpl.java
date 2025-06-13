package dev.kuku.vfl.api.buffer;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import dev.kuku.vfl.internal.VisFlowLogBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultBufferImpl implements VisFlowLogBuffer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBufferImpl.class);

    private final List<VflLogDataType> logs;
    private final List<VflBlockDataType> blocks;
    private final int blockBufferSize;
    private final int logBufferSize;
    private final Executor flushExecutor;

    // Prevent multiple concurrent flushes
    private final AtomicBoolean isFlushingLogs = new AtomicBoolean(false);
    private final AtomicBoolean isFlushingBlocks = new AtomicBoolean(false);

    // For shutdown handling
    private volatile boolean isShuttingDown = false;

    public DefaultBufferImpl(int blockBufferSize, int logBufferSize) {
        this.logs = Collections.synchronizedList(new ArrayList<>());
        this.blocks = Collections.synchronizedList(new ArrayList<>());
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        this.flushExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "vfl-buffer-flush");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
    }

    @Override
    public CompletableFuture<Void> pushLogToBuffer(VflLogDataType log) {
        if (isShuttingDown) {
            logger.warn("Attempted to add log during shutdown, ignoring");
            return CompletableFuture.completedFuture(null);
        }

        logs.add(log);

        // Fire and forget - only flush if buffer is full
        if (logs.size() >= logBufferSize) {
            flushLogsAsync();
        }

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> pushBlockToBuffer(VflBlockDataType block) {
        if (isShuttingDown) {
            logger.warn("Attempted to add block during shutdown, ignoring");
            return CompletableFuture.completedFuture(null);
        }

        blocks.add(block);

        // Fire and forget - only flush if buffer is full
        if (blocks.size() >= blockBufferSize) {
            flushBlocksAsync();
        }

        return CompletableFuture.completedFuture(null);
    }

    private void flushLogsAsync() {
        // Prevent multiple concurrent log flushes
        if (!isFlushingLogs.compareAndSet(false, true)) {
            logger.debug("Log flush already in progress, skipping");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                flushLogs();
            } finally {
                isFlushingLogs.set(false);
            }
        }, flushExecutor);
    }

    private void flushBlocksAsync() {
        // Prevent multiple concurrent block flushes
        if (!isFlushingBlocks.compareAndSet(false, true)) {
            logger.debug("Block flush already in progress, skipping");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                flushBlocks();
            } finally {
                isFlushingBlocks.set(false);
            }
        }, flushExecutor);
    }

    private void flushLogs() {
        if (logs.isEmpty()) {
            return;
        }

        logger.debug("Flushing {} logs", logs.size());

        // Create a snapshot of current logs and clear the buffer
        List<VflLogDataType> logsToFlush;
        synchronized (logs) {
            logsToFlush = new ArrayList<>(logs);
            logs.clear();
        }

        try {
            // TODO: Replace with actual persistence logic
            // For now, simulate some work
            Thread.sleep(100);

            logger.debug("Successfully flushed {} logs", logsToFlush.size());

            // Here you would typically:
            // - Send to database
            // - Write to file
            // - Send to external service
            // - etc.

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Log flush interrupted", e);

            // Re-add logs back to buffer if flush failed
            synchronized (logs) {
                logs.addAll(0, logsToFlush);
            }
        } catch (Exception e) {
            logger.error("Failed to flush logs", e);

            // Re-add logs back to buffer if flush failed
            synchronized (logs) {
                logs.addAll(0, logsToFlush);
            }
        }
    }

    private void flushBlocks() {
        if (blocks.isEmpty()) {
            return;
        }

        logger.debug("Flushing {} blocks", blocks.size());

        // Create a snapshot of current blocks and clear the buffer
        List<VflBlockDataType> blocksToFlush;
        synchronized (blocks) {
            blocksToFlush = new ArrayList<>(blocks);
            blocks.clear();
        }

        try {
            // TODO: Replace with actual persistence logic
            Thread.sleep(50);

            logger.debug("Successfully flushed {} blocks", blocksToFlush.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Block flush interrupted", e);

            // Re-add blocks back to buffer if flush failed
            synchronized (blocks) {
                blocks.addAll(0, blocksToFlush);
            }
        } catch (Exception e) {
            logger.error("Failed to flush blocks", e);

            // Re-add blocks back to buffer if flush failed
            synchronized (blocks) {
                blocks.addAll(0, blocksToFlush);
            }
        }
    }

    /**
     * Force flush all remaining logs and blocks.
     * This should be called during application shutdown.
     */
    public CompletableFuture<Void> shutdown() {
        logger.info("Shutting down buffer, flushing remaining data");
        isShuttingDown = true;

        // Force flush everything
        CompletableFuture<Void> logFlush = CompletableFuture.runAsync(() -> {
            while (isFlushingLogs.get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            flushLogs();
        }, flushExecutor);

        CompletableFuture<Void> blockFlush = CompletableFuture.runAsync(() -> {
            while (isFlushingBlocks.get()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            flushBlocks();
        }, flushExecutor);

        return CompletableFuture.allOf(logFlush, blockFlush)
                .whenComplete((result, throwable) -> {
                    if (flushExecutor instanceof ExecutorService) {
                        ((ExecutorService) flushExecutor).shutdown();
                    }
                    logger.info("Buffer shutdown complete");
                });
    }

    // Getters for monitoring/debugging
    public int getLogBufferSize() {
        return logs.size();
    }

    public int getBlockBufferSize() {
        return blocks.size();
    }
}