package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;

import java.util.concurrent.CompletableFuture;

public interface VFLBuffer {
    /**
     * Add a log entry to the buffer (fire-and-forget).
     */
    void pushLogToBuffer(LogData log);

    /**
     * Add a block entry to the buffer (fire-and-forget).
     */
    void pushBlockToBuffer(BlockData block);

    /**
     * Graceful shutdown - this IS appropriate to return a future.
     */
    CompletableFuture<Void> flushAllAsync();
}