package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.buffer.abstracts.VFLBufferWithFlushHandlerBase;
import dev.kuku.vfl.core.buffer.flushHandler.VFLFlushHandler;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.List;
import java.util.Map;

/**
 * A {@link dev.kuku.vfl.core.buffer.VFLBuffer} implementation that flushes data
 * <b>synchronously</b> in the calling thread.
 *
 * <p>When the buffer is full (or {@link #flush()} is called), any collected logs, blocks,
 * and block lifecycle events are sent immediately to the configured {@link VFLFlushHandler}.
 * The call will block until the destination processing is completed.</p>
 *
 * <h2>Key characteristics:</h2>
 * <ul>
 *   <li>Simple, predictable execution — no background threads</li>
 *   <li>Flush overhead is paid by the thread calling the flush</li>
 *   <li>Useful when:
 *     <ul>
 *       <li>You require guaranteed ordering and delivery before proceeding</li>
 *       <li>You prefer minimal concurrency complexity</li>
 *       <li>Logs volume is small and latency is acceptable</li>
 *     </ul>
 *   </li>
 *   <li>Not recommended if flush operations involve slow I/O or heavy processing,
 *       as this will directly impact caller performance</li>
 * </ul>
 *
 * <h2>Example:</h2>
 * <pre>{@code
 * VFLFlushHandler handler = new MyHttpFlushHandler();
 * VFLBuffer buffer = new SynchronousBuffer(100, handler);
 * // Pass this buffer into your VFL initializer config
 * }</pre>
 */
public class SynchronousBuffer extends VFLBufferWithFlushHandlerBase {

    /**
     * @param bufferSize   Max number of buffered items before auto-flush
     * @param flushHandler Destination handler that will receive flushed data
     */
    public SynchronousBuffer(int bufferSize, VFLFlushHandler flushHandler) {
        super(bufferSize, flushHandler);
    }

    /**
     * Performs a blocking flush in the calling thread
     * by directly invoking {@link #performOrderedFlush(List, List, Map, Map)}.
     */
    @Override
    protected void executeFlushAll(List<Log> logs,
                                   List<Block> blocks,
                                   Map<String, Long> blockStarts,
                                   Map<String, BlockEndData> blockEnds) {
        performOrderedFlush(logs, blocks, blockStarts, blockEnds);
    }
}
