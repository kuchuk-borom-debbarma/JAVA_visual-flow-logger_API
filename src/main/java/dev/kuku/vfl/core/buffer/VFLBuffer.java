package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.dtos.BlockEndData;
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
     * Log that has started needs to be pushed
     */
    void pushLogStartToBuffer(String blockId, long timestamp);

    /**
     * Log that has ended needs to be pushed
     */
    void pushLogEndToBuffer(String blockId, BlockEndData endTimeAndMessage);

    /**
     * Will flush all pending data and then shutdown
     */
    void flushAndClose();
}