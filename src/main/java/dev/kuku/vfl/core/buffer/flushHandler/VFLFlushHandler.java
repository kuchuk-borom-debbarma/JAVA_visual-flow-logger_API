package dev.kuku.vfl.core.buffer.flushHandler;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.List;

public interface VFLFlushHandler {
    boolean pushLogsToServer(List<Log> logs);

    boolean pushBlocksToServer(List<Block> blocks);
}
