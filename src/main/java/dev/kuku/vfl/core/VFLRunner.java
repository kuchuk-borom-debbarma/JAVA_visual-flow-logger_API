package dev.kuku.vfl.core;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.VFLBlockContext;

import static dev.kuku.vfl.core.helpers.Util.UID;

public abstract class VFLRunner {
    public VFLBlockContext initRootCtx(String blockName, VFLBuffer buffer) {
        Block rootBlock = new Block(UID(), null, blockName);
        buffer.pushBlockToBuffer(rootBlock);
        return new VFLBlockContext(rootBlock, buffer, null);
    }
}