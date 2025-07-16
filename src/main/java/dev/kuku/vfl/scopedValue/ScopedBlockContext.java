package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * To be used to store log data that has a clear boundary defined.
 * <p>
 * Used by ScopedValue to create bounded data
 */
class ScopedBlockContext {
    public final BlockData blockInfo;
    public final VFLBuffer buffer;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public LogData currentLog;

    ScopedBlockContext(BlockData blockInfo, VFLBuffer buffer) {
        this.blockInfo = blockInfo;
        this.buffer = buffer;
    }

    @Override
    public String toString() {
        return "BoundedLogData{" +
                "blockInfo=" + blockInfo +
                ", buffer=" + buffer +
                ", blockStarted=" + blockStarted +
                ", currentLog=" + currentLog +
                '}';
    }
}
