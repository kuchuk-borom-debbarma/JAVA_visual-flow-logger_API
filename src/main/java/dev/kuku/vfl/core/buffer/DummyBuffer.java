package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import org.javatuples.Pair;

public class DummyBuffer implements VFLBuffer {
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
    public void pushLogEndToBuffer(String blockId, Pair<Long, String> endTimeAndMessage) {

    }

    @Override
    public void flushAndClose() {

    }
}
