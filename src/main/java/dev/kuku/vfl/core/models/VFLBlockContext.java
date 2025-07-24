package dev.kuku.vfl.core.models;

import dev.kuku.vfl.core.buffer.VFLBuffer;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class VFLBlockContext {
    public final Block blockInfo;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public String currentLogId;
    public final VFLBuffer buffer;
    public final Set<String> allowedLogTypes;

    public VFLBlockContext(Block blockInfo, VFLBuffer buffer, Set<String> allowedLogTypes) {
        this.blockInfo = blockInfo;
        this.buffer = buffer;
        this.allowedLogTypes = allowedLogTypes;
    }
}
