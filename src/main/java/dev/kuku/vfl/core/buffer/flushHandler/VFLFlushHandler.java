package dev.kuku.vfl.core.buffer.flushHandler;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;

public interface VFLFlushHandler {
    boolean pushLogsToServer(List<Log> logs);

    boolean pushBlocksToServer(List<Block> blocks);

    boolean pushBlockStartsToServer(Map<String, Long> blockStarts);

    boolean pushBlockEndsToServer(Map<String, Pair<Long, String>> blockEnds);

    void closeFlushHandler();
}
