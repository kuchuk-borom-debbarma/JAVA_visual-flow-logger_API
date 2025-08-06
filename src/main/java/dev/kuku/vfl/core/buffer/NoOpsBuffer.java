package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

public class NoOpsBuffer implements VFLBuffer {
    @Override
    public void pushLogToBuffer(Log log) {

    }

    @Override
    public void pushBlockToBuffer(Block block) {

    }

    @Override
    public void pushLogStartToBuffer(String blockId, long timestamp) {

    }

    @Override
    public void pushLogEndToBuffer(String blockId, BlockEndData endData) {

    }

    @Override
    public void flushAndClose() {

    }
}
