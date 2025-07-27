package dev.kuku.vfl.core.vfl_abstracts.runner;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

public abstract class VFLRunner {
    protected static VFLBlockContext initRootCtx(String operationName, VFLBuffer buffer) {
        Block rootBlock = VFL.VFLHelper.CreateBlockAndPush2Buffer(operationName, null, buffer);
        return new VFLBlockContext(rootBlock, buffer);
    }
}
