package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

import java.util.concurrent.atomic.AtomicBoolean;

class ScopedLoggerData {
    public final BlockData blockInfo;
    public final VFLBuffer buffer;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public LogData currentLog;

    ScopedLoggerData(BlockData blockInfo, VFLBuffer buffer) {
        this.blockInfo = blockInfo;
        this.buffer = buffer;
    }
}
