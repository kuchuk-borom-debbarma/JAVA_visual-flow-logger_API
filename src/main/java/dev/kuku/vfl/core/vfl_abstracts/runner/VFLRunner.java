package dev.kuku.vfl.core.vfl_abstracts.runner;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;

public abstract class VFLRunner {
    protected static VFLBlockContext initRootCtx(String operationName, VFLBuffer buffer) {
        Block rootBlock = VFLHelper.CreateBlockAndPush2Buffer(operationName, null, buffer);
        return new VFLBlockContext(rootBlock, buffer);
    }
}
