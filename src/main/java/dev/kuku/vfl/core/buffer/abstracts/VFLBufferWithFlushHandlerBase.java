package dev.kuku.vfl.core.buffer.abstracts;

import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.List;
import java.util.Map;

/**
 * Abstract extension of {@link VFLBufferBase} that integrates with a {@link VFLFlushHandler}.
 *
 * <p>This provides the common structure for VFL buffers that:
 * <ul>
 *   <li>Batch incoming blocks, logs, and start/end events (via {@link VFLBufferBase})</li>
 *   <li>Use a {@link VFLFlushHandler} to actually send these batched items to a destination
 *       such as a server, message queue, or file store</li>
 *   <li>Allow subclasses to decide the execution strategy (synchronous, asynchronous, threaded, etc.)</li>
 * </ul>
 *
 * <h2>Key Points:</h2>
 * <ul>
 *   <li>{@link #executeFlushAll(List, List, Map, Map)} must be implemented by subclasses
 *       to define how the flush will be performed.</li>
 *   <li>{@link #performOrderedFlush(List, List, Map, Map)} is provided as a utility
 *       to flush in the correct sequence:
 *       <ol>
 *           <li>Blocks (structure)</li>
 *           <li>Block start events (timestamps)</li>
 *           <li>Block end events (timestamps + end messages)</li>
 *           <li>Log entries (messages)</li>
 *       </ol>
 *   </li>
 *   <li>{@link #flush()} closes the flush handler after flushing remaining data —
 *       suitable for shutdown hooks.</li>
 * </ul>
 *
 * <h2>Typical Usage:</h2>
 * <pre>{@code
 * public class AsyncHttpVFLBuffer extends VFLBufferWithFlushHandlerBase {
 *     public AsyncHttpVFLBuffer(int size, VFLFlushHandler handler) {
 *         super(size, handler);
 *     }
 *
 *     @Override
 *     protected void executeFlushAll(List<Log> logs,
 *                                     List<Block> blocks,
 *                                     Map<String, Long> blockStarts,
 *                                     Map<String, BlockEndData> blockEnds) {
 *         // Example: send in another thread
 *         executor.submit(() -> performOrderedFlush(logs, blocks, blockStarts, blockEnds));
 *     }
 * }
 * }</pre>
 */
public abstract class VFLBufferWithFlushHandlerBase extends VFLBufferBase {

    /** Responsible for pushing buffered data to the destination (e.g., server, DB, queue) */
    protected final VFLFlushHandler flushHandler;

    /**
     * @param bufferSize   maximum combined items before triggering automatic flush
     * @param flushHandler handler that knows how to send the batched data to its destination
     */
    public VFLBufferWithFlushHandlerBase(int bufferSize, VFLFlushHandler flushHandler) {
        super(bufferSize);
        this.flushHandler = flushHandler;
    }

    /**
     * Final override from {@link VFLBufferBase} that delegates the
     * flush operation to {@link #executeFlushAll(List, List, Map, Map)}.
     */
    @Override
    protected final void onFlushAll(List<Log> logs,
                                    List<Block> blocks,
                                    Map<String, Long> blockStarts,
                                    Map<String, BlockEndData> blockEnds) {
        executeFlushAll(logs, blocks, blockStarts, blockEnds);
    }

    /**
     * Flush remaining data and close the flush handler.
     * <p>Intended to be called during graceful shutdown.</p>
     */
    @Override
    public void flush() {
        super.flush(); // Flush any pending batches
        flushHandler.closeFlushHandler(); // Cleanup flush handler resources
    }

    /**
     * Subclasses must implement this to define how the flush operation will be performed.
     *
     * @param logs        snapshot of logs to send
     * @param blocks      snapshot of blocks to send
     * @param blockStarts snapshot of block start timestamps
     * @param blockEnds   snapshot of block end data
     */
    protected abstract void executeFlushAll(List<Log> logs,
                                            List<Block> blocks,
                                            Map<String, Long> blockStarts,
                                            Map<String, BlockEndData> blockEnds);

    /**
     * Convenience method to flush in correct dependency order:
     * <ol>
     *     <li>Blocks → parent structure for events/logs</li>
     *     <li>Block starts → mark when blocks began</li>
     *     <li>Block ends → mark when blocks finished</li>
     *     <li>Logs → messages linked to existing blocks</li>
     * </ol>
     * Subclasses can call this from {@link #executeFlushAll(List, List, Map, Map)}
     * to enforce ordering.
     */
    protected final void performOrderedFlush(List<Log> logs,
                                             List<Block> blocks,
                                             Map<String, Long> blockStarts,
                                             Map<String, BlockEndData> blockEnds) {
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
