package dev.kuku.vfl.buffer;

import dev.kuku.dto.BlockDTO;
import dev.kuku.dto.LogDTO;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.util.ApiClient;
import dev.kuku.vfl.util.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class DefaultBufferImpl implements VFLBuffer {
    private static final Logger logger = LoggerFactory.getLogger(DefaultBufferImpl.class);
    private final ApiClient apiClient = new ApiClient();
    private final JSONUtil jsonUtil = new JSONUtil();
    private final List<LogData> logs;
    private final List<BlockData> blocks;
    private final int blockBufferSize;
    private final int logBufferSize;
    private final Executor flushExecutor;
    // Volatile because we want the value directly from source and not the per-thread cached value
    private boolean isShuttingDown = false;

    public DefaultBufferImpl(int blockBufferSize, int logBufferSize) {
        //TODO use ring buffer in future
        this.logs = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.blockBufferSize = blockBufferSize;
        this.logBufferSize = logBufferSize;
        //We will run our flush operation in this thread
        this.flushExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void pushLogToBuffer(LogData log) {
        if (isShuttingDown) {
            logger.warn("Attempted to add log during shutdown, ignoring");
            return;
        }
        //Even though it may look like allowing multiple pushes parallelly should be fine, it really is not.
        //The values such as size, etc. May end up being different, and two elements may get pushed to the same index.
        //Using volatile keyword is also not going to do anything because we are not only reading but modifying.
        synchronized (logs) {
            logs.add(log);
            //Consistent size as it's locked
            checkAndFlushIfNeeded();
        }
    }

    @Override
    public void pushBlockToBuffer(BlockData block) {
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
            checkAndFlushIfNeeded();
        }
    }

    private void checkAndFlushIfNeeded() {
        // Check if either buffer has reached its individual limit
        boolean shouldFlush;
        synchronized (logs) {
            synchronized (blocks) {
                shouldFlush = logs.size() >= logBufferSize ||
                        blocks.size() >= blockBufferSize ||
                        (logs.size() + blocks.size()) >= (logBufferSize + blockBufferSize);
            }
        }

        if (shouldFlush) {
            flushExecutor.execute(() -> {
                flushBlocks();
                flushLogs();
            });
        }
    }

    private void flushBlocks() {
        List<BlockData> blocksToFlush;
        //Copy the blocks in local variable and then release it.
        synchronized (blocks) {
            if (blocks.isEmpty()) {
                return;
            }
            logger.debug("Flushing {} blocks", blocks.size());
            blocksToFlush = new ArrayList<>(blocks);
            blocks.clear();
        }
        try {
            var blockDTOs = blocksToFlush.stream().map(b -> new BlockDTO(b.getId(), b.getBlockName(), b.getParentBlockId().orElse(null))).toList();
            var resp = apiClient.post("http://localhost:8080/api/v1/block/", jsonUtil.toJson(blockDTOs), Boolean.class);
        } catch (Exception e) {
            logger.error("Failed to save blocks to database", e);
            //Re-add failed blocks back to the buffer
            synchronized (blocks) {
                blocks.addAll(0, blocksToFlush); // Add at beginning to maintain some ordering
            }
        }
    }

    private void flushLogs() {
        List<LogData> logsToFlush;
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
            var logsToAdd = logsToFlush.stream().map(l -> new LogDTO(l.getId(),
                    l.getParentLogId().orElse(null),
                    l.getBlockId(),
                    l.getLogValue().orElse(null),
                    l.getLogType(),
                    l.getBlockPointersString().orElse(null),
                    Instant.now().toEpochMilli())).toList();
            apiClient.post("http://localhost:8080/api/v1/vfl/logs", jsonUtil.toJson(logsToAdd), Boolean.class);
        } catch (Exception e) {
            logger.error("Failed to save logs to database", e);
            //Re-add failed logs back to the buffer
            synchronized (logs) {
                logs.addAll(0, logsToFlush); // Add at the beginning to maintain some ordering
            }
        }
    }

    @Override
    public CompletableFuture<Void> flushAllAsync() {
        isShuttingDown = true;
        logger.info("Flushing remaining data");
        return CompletableFuture.runAsync(() -> {
            flushBlocks();
            flushLogs();
            logger.info("Final flush completed");
        }, flushExecutor);
    }
}