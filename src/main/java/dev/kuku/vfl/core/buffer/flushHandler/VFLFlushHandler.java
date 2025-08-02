package dev.kuku.vfl.core.buffer.flushHandler;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.List;
import java.util.Map;

public interface VFLFlushHandler {
    boolean pushLogsToServer(List<Log> logs);

    boolean pushBlocksToServer(List<Block> blocks);

    boolean pushBlockStartsToServer(Map<String, Long> blockStarts);

    boolean pushBlockEndsToServer(Map<String, BlockEndData> blockEnds);

    void closeFlushHandler();
}
