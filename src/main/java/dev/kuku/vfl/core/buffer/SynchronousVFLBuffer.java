package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;

public class SynchronousVFLBuffer extends VFLBufferWithFlushHandlerBase {
    public SynchronousVFLBuffer(int bufferSize, VFLFlushHandler flushHandler) {
        super(bufferSize, flushHandler);
    }

    @Override
    protected void executeFlushAll(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, Pair<Long, String>> blockEnds) {
        // Execute synchronously - directly call the ordered flush
        performOrderedFlush(logs, blocks, blockStarts, blockEnds);
    }
}