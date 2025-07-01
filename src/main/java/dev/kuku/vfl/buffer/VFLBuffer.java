package dev.kuku.vfl.buffer;

import dev.kuku.vfl.models.VflBlockDataType;
import dev.kuku.vfl.models.VflLogDataType;

import java.util.concurrent.CompletableFuture;

public interface VFLBuffer {
    /**
     * Add a log entry to the buffer (fire-and-forget).
     */
    void pushLogToBuffer(VflLogDataType log);

    /**
     * Add a block entry to the buffer (fire-and-forget).
     */
    void pushBlockToBuffer(VflBlockDataType block);

    /**
     * Graceful shutdown - this IS appropriate to return a future.
     */
    CompletableFuture<Void> flushAllAsync();
}