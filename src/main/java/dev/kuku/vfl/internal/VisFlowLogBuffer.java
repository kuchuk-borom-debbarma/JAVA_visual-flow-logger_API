package dev.kuku.vfl.internal;

import dev.kuku.vfl.api.models.VflBlockDataType;
import dev.kuku.vfl.api.models.VflLogDataType;

import java.util.concurrent.CompletableFuture;

public interface VisFlowLogBuffer {
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
    CompletableFuture<Void> shutdown();
}