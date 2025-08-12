package dev.kuku.vfl.core.buffer.flushHandler;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.List;
import java.util.Map;

/**
 * Destination handler for flushing batched VFL trace data.
 *
 * <p>Implementations of this interface are responsible for taking
 * batches of logs, blocks, and lifecycle events from a {@link dev.kuku.vfl.core.buffer.VFLBuffer}
 * (e.g., {@link dev.kuku.vfl.core.buffer.SynchronousBuffer} or
 * {@link dev.kuku.vfl.core.buffer.AsyncBuffer}) and delivering them
 * to a final destination such as a remote server, message queue, database, or file system.
 *
 * <h2>Contract:</h2>
 * <ul>
 *   <li>Each method receives a <b>batch snapshot</b> of data to send.</li>
 *   <li>Must be <b>thread‑safe</b> — buffers may flush from multiple threads concurrently.</li>
 *   <li>Return {@code true} if the data was successfully delivered, {@code false} for a recoverable failure
 *       (allowing buffer policies to retry if implemented).</li>
 *   <li>Methods must return promptly — avoid long blocking unless acceptable by design (e.g., in synchronous flushing).</li>
 * </ul>
 *
 * <h2>Flush Ordering:</h2>
 * Buffers may use {@code performOrderedFlush()} to ensure flushing happens in this sequence:
 * <ol>
 *     <li>{@link #pushBlocksToServer(List)}</li>
 *     <li>{@link #pushBlockStartsToServer(Map)}</li>
 *     <li>{@link #pushBlockEndsToServer(Map)}</li>
 *     <li>{@link #pushLogsToServer(List)}</li>
 * </ol>
 * This ensures all references (IDs) exist before related events are sent.
 *
 * <h2>Lifecycle:</h2>
 * <ul>
 *   <li>{@link #closeFlushHandler()} will be called during shutdown to release resources
 *       (e.g., close network connections, stop threads).</li>
 *   <li>Implementers should make {@code closeFlushHandler()} idempotent and safe to call multiple times.</li>
 * </ul>
 */
public interface VFLFlushHandler {

    /**
     * Push a batch of logs to the destination.
     *
     * @param logs list of log entries to send
     * @return true if successfully delivered, false otherwise
     */
    boolean pushLogsToServer(List<Log> logs);

    /**
     * Push a batch of block definitions to the destination.
     *
     * @param blocks block metadata to send
     * @return true if successfully delivered, false otherwise
     */
    boolean pushBlocksToServer(List<Block> blocks);

    /**
     * Push a mapping of block IDs to their start timestamps.
     *
     * @param blockStarts map where key = blockId, value = epoch millis start time
     * @return true if successfully delivered, false otherwise
     */
    boolean pushBlockStartsToServer(Map<String, Long> blockStarts);

    /**
     * Push a mapping of block IDs to their end data.
     *
     * @param blockEnds map where key = blockId, value = end metadata (timestamp + optional message)
     * @return true if successfully delivered, false otherwise
     */
    boolean pushBlockEndsToServer(Map<String, BlockEndData> blockEnds);

    /**
     * Release any resources used by this flush handler.
     * <p>Called once during shutdown; should close connections and stop background tasks.</p>
     */
    void closeFlushHandler();
}
