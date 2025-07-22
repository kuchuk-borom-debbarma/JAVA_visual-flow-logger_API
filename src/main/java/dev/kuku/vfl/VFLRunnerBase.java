package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public abstract class VFLRunnerBase {
    protected static VFLBlockContext initRootBlock(String blockName, VFLBuffer buffer) {
        BlockData rootBlockData = BlockHelper.CreateBlockDataAndPush(generateUID(), blockName, null, buffer);
        return new VFLBlockContext(rootBlockData, buffer);
    }

    protected static void cleanup(VFLBuffer buffer) {
        buffer.flushAndClose();
    }
}