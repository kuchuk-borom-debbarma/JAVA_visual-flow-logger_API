package dev.kuku.vfl.api.buffer;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Default Buffer implementation that uses a different thread to flush. <br>
 * Flush operations are considered critical section and thus is locked to prevent mutation during flushing.
 */
//TODO use interface and builder for defaultBufferImpl so that devs can utilize this while maintaining core functionality
public class DefaultBufferImpl implements VisFlowLogBuffer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBufferImpl.class);

    private final List<VflLogDataType> logs;
    private final List<VflBlockDataType> blocks;
    private final int blockBufferSize;
    private final int logBufferSize;
    private final Executor flushExecutor;
    // Volatile because we want the value directly from source and not the per-thread cached value
    private volatile boolean isShuttingDown = false;

    public DefaultBufferImpl(int blockBufferSize, int logBufferSize) throws SQLException {
        //TODO use ring buffer in future
        this.logs = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        //We will run our flush operation in this thread
        this.flushExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "vfl-buffer-flush" + Instant.now().toEpochMilli());
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
        //Even though it may look like allowing multiple pushes parallelly should be fine, it really is not.
        //The values such as size, etc may end up being different, and two elements may get pushed to the same index.
        //Using volatile keyword is also not going to do anything because we are not only reading but modifying.
        synchronized (logs) {
            logs.add(log);
            //Consistent size as it's locked
            if (logs.size() >= logBufferSize) {
                flushLogsAsync();
            }
        }
    }

    @Override
    public void pushBlockToBuffer(VflBlockDataType block) {
        if (isShuttingDown) {
            logger.warn("Attempted to add block during shutdown, ignoring");
            return;
        }
        //Even though it may look like allowing multiple pushes parallelly should be fine, it really is not.
        //The values such as size, etc may end up being different, and two elements may get pushed to the same index.
        //Using volatile keyword is also not going to do anything because we are not only reading but modifying.
        synchronized (blocks) {
            blocks.add(block);
            //Consistent size as its locked
            if (blocks.size() >= blockBufferSize) {
                flushBlocksAsync();
            }
        }
    }

    private CompletableFuture<Void> flushLogsAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                flushLogs();
            } catch (Exception e) {
                logger.error("Async log flush failed", e);
            }
        }, flushExecutor);
    }

    private CompletableFuture<Void> flushBlocksAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                flushBlocks();
            } catch (Exception e) {
                logger.error("Async block flush failed", e);
            }
        }, flushExecutor);
    }

    private void flushLogs() {
        List<VflLogDataType> logsToFlush;
        synchronized (logs) {
            if (logs.isEmpty()) {
                return;
            }
            logger.debug("Flushing {} logs", logs.size());
            //copy the logs to the local variable and empty the main one. It can be released
            logsToFlush = new ArrayList<>(logs);
            logs.clear();
        }
        //Critical section over. Now another thread can safely mutate logs
        try {
            //TODO save using SQL
        } catch (Exception e) {
            logger.error("Failed to save logs to database", e);
            //Re-add failed logs back to the buffer
            synchronized (logs) {
                logs.addAll(0, logsToFlush); // Add at the beginning to maintain some ordering
            }
        }
    }

    private void flushBlocks() {
        List<VflBlockDataType> blocksToFlush;
        synchronized (blocks) {
            if (blocks.isEmpty()) {
                return;
            }
            logger.debug("Flushing {} blocks", blocks.size());
            blocksToFlush = new ArrayList<>(blocks);
            blocks.clear(); // FIX: Clear the original list
        }
        try {
            //TODO save using SQL
        } catch (Exception e) {
            logger.error("Failed to save blocks to database", e);
            //Re-add failed blocks back to the buffer
            synchronized (blocks) {
                blocks.addAll(0, blocksToFlush); // Add at beginning to maintain some ordering
            }
        }
    }

    @Override
    public CompletableFuture<Void> flushAllAsync() {
        logger.info("Flushing remaining data");
        isShuttingDown = true;
        return CompletableFuture.allOf(flushBlocksAsync(), flushLogsAsync());
    }

    // Getters for monitoring/debugging - now thread-safe
    public int getLogBufferSize() {
        synchronized (logs) {
            return logs.size();
        }
    }

    public int getBlockBufferSize() {
        synchronized (blocks) {
            return blocks.size();
        }
    }

}