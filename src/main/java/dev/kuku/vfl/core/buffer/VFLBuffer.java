package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

public interface VFLBuffer {
    /**
     * Add a log entry to the buffer (fire-and-forget).
     */
    void pushLogToBuffer(Log log);

    /**
     * Add a block entry to the buffer (fire-and-forget).
     */
    void pushBlockToBuffer(Block block);

    /**
     * Will flush all pending data and then shutdown
     */
    void flushAndClose();
}