package dev.kuku.vfl.core.buffer.flusher;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

import java.util.ArrayList;
import java.util.List;

public class InMemoryFlusherImpl implements VFLFlusher {
    public List<LogData> logs = new ArrayList<>();
    public List<BlockData> blocks = new ArrayList<>();

    @Override
    public boolean pushLogsToServer(List<LogData> logs) {
        this.logs.addAll(logs);
        return true;
    }

    @Override
    public boolean pushBlocksToServer(List<BlockData> blocks) {
        this.blocks.addAll(blocks);
        return true;
    }
}
