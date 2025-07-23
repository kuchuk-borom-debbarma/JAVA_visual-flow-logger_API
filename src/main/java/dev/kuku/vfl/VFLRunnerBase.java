package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.VFLBlockContext;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public abstract class VFLRunnerBase {
    protected static VFLBlockContext initRootBlock(String blockName, VFLBuffer buffer) {
        Block rootBlock = BlockHelper.CreateBlockDataAndPush(generateUID(), blockName, null, buffer);
        return new VFLBlockContext(rootBlock, buffer);
    }

    protected static void cleanup(VFLBuffer buffer) {
        buffer.flushAndClose();
    }
}