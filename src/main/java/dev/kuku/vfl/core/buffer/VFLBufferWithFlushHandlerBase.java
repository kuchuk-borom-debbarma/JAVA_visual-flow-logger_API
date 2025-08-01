package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;

public abstract class VFLBufferWithFlushHandlerBase extends VFLBufferBase {
    protected final VFLFlushHandler flushHandler;

    public VFLBufferWithFlushHandlerBase(int bufferSize, VFLFlushHandler flushHandler) {
        super(bufferSize);
        this.flushHandler = flushHandler;
    }

    @Override
    protected final void onFlushAll(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, Pair<Long, String>> blockEnds) {
        // Delegate to subclass for execution strategy (sync vs async)
        executeFlushAll(logs, blocks, blockStarts, blockEnds);
    }

    @Override
    public void flushAndClose() {
        super.flushAndClose(); // Flush any remaining data
        flushHandler.closeFlushHandler(); // Clean up the flush handler
    }

    // Abstract method for subclasses to implement their execution strategy
    protected abstract void executeFlushAll(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, Pair<Long, String>> blockEnds);

    // Helper method to perform ordered flushing - can be called by subclasses
    protected final void performOrderedFlush(List<Log> logs, List<Block> blocks, Map<String, Long> blockStarts, Map<String, Pair<Long, String>> blockEnds) {
        // Enforced flush order: blocks -> block starts -> block ends -> logs
        if (!blocks.isEmpty()) {
            flushHandler.pushBlocksToServer(blocks);
        }
        if (!blockStarts.isEmpty()) {
            flushHandler.pushBlockStartsToServer(blockStarts);
        }
        if (!blockEnds.isEmpty()) {
            flushHandler.pushBlockEndsToServer(blockEnds);
        }
        if (!logs.isEmpty()) {
            flushHandler.pushLogsToServer(logs);
        }
    }
}
