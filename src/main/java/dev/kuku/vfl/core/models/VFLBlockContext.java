package dev.kuku.vfl.core.models;

import java.util.concurrent.atomic.AtomicBoolean;

public class VFLBlockContext {
    public final BlockData blockInfo;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public String currentLogId;

    public VFLBlockContext(BlockData blockInfo) {
        this.blockInfo = blockInfo;
    }
}
