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

public class DefaultBufferImpl implements VisFlowLogBuffer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBufferImpl.class);

    private final List<VflLogDataType> logs;
    private final List<VflBlockDataType> blocks;
    private final int blockBufferSize;
    private final int logBufferSize;
    private final Executor flushExecutor;

    // For shutdown handling
    private volatile boolean isShuttingDown = false;

    public DefaultBufferImpl(int blockBufferSize, int logBufferSize) {
        this.logs = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        //We will run our flush operation in this thread
        this.flushExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "vfl-buffer-flush");
            t.setDaemon(true); // Don't prevent JVM shutdown
            return t;
        });
    }

    @Override
    public void pushLogToBuffer(VflLogDataType log) {
        if (isShuttingDown) {
            logger.warn("Attempted to add log during shutdown, ignoring");
            return;
        }
        logs.add(log);
        if (logs.size() >= logBufferSize) {
            flushLogsAsync();
        }
    }

    @Override
    public void pushBlockToBuffer(VflBlockDataType block) {
        if (isShuttingDown) {
            logger.warn("Attempted to add block during shutdown, ignoring");
            return;
        }
        blocks.add(block);
        if (blocks.size() >= blockBufferSize) {
            flushBlocksAsync();
        }
    }

    private synchronized void flushLogsAsync() {
        //synchronized stops others to use this function when it is already running
        CompletableFuture.runAsync(() -> {
            try {
                flushLogs();
            } catch (Exception e) {
                logger.error("Async log flush failed", e);
            }
        }, flushExecutor);
    }

    private synchronized void flushBlocksAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                flushBlocks();
            } catch (Exception e) {
                logger.error("Async block flush failed", e);
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
            Thread.sleep(100);
            logger.debug("Successfully flushed {} logs", logsToFlush.size());

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
     * <p>
     * This method DOES return a CompletableFuture because shutdown
     * is a coordination operation that callers need to wait for.
     */
    @Override
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