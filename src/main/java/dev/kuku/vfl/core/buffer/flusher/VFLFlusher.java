package dev.kuku.vfl.core.buffer.flusher;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;

import java.util.List;

public interface VFLFlusher {
    boolean pushLogsToServer(List<LogData> logs);

    boolean pushBlocksToServer(List<BlockData> blocks);
}
