package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

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
     * Will flush all pending data and then shutdown
     */
    void flushAndClose();
}