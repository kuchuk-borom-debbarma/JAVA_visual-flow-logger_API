package dev.kuku.vfl.core.models;

import dev.kuku.vfl.core.buffer.VFLBuffer;

import java.util.concurrent.atomic.AtomicBoolean;

public class VFLBlockContext {
    public final BlockData blockInfo;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public String currentLogId;
    public final VFLBuffer buffer;

    public VFLBlockContext(BlockData blockInfo, VFLBuffer buffer) {
        this.blockInfo = blockInfo;
        this.buffer = buffer;
    }
}
