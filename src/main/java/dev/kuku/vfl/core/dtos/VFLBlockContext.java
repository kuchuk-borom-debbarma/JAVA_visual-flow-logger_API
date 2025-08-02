package dev.kuku.vfl.core.dtos;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class VFLBlockContext {
    public final Block blockInfo;
    public final AtomicBoolean blockStarted = new AtomicBoolean(false);
    public final VFLBuffer buffer;
    public String currentLogId;
}
