package dev.kuku.vfl.core.buffer;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

/**
 * Abstraction for buffering Visual Flow Logger (VFL) trace data.
 *
 * <p>Implementations of this interface are responsible for temporarily
 * storing blocks and log entries generated during execution, before
 * they are persisted, sent over the network, or otherwise processed.
 *
 * <p>VFL calls these methods in a <b>fire-and-forget</b> manner —
 * your implementation must be thread‑safe and non‑blocking as much as possible.
 * Slow or blocking implementations can negatively affect application performance.
 *
 * <h2>Typical Implementations:</h2>
 * <ul>
 *   <li>In‑memory async buffer with background flush to a datastore</li>
 *   <li>Direct streaming of entries to Kafka or another message broker</li>
 *   <li>Writing to a local file for offline analysis</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * Implementations <b>must</b> be safe for concurrent calls from
 * multiple application threads.
 */
public interface VFLBuffer {

    /**
     * Buffer a single log entry for the current block.
     *
     * <p>Called whenever the user logs a message (INFO/WARN/ERROR, etc.).
     * Should enqueue or persist the log entry without blocking the caller.
     *
     * @param log the log entry to store
     */
    void pushLogToBuffer(Log log);

    /**
     * Buffer a block creation event.
     *
     * <p>Called whenever a new block is created (root, sub-block,
     * continuation, event listener, etc.).
     *
     * @param block the block metadata to store
     */
    void pushBlockToBuffer(Block block);

    /**
     * Buffer the "start" event for an existing block with its timestamp.
     *
     * <p>Called when VFL marks a block as started
     * (before any log messages are written to it).
     *
     * @param blockId   ID of the block being started
     * @param timestamp epoch millis of the start time
     */
    void pushLogStartToBuffer(String blockId, long timestamp);

    /**
     * Buffer the "end" event for an existing block.
     *
     * <p>Called when a block is closed, typically including
     * its end timestamp and optional final message.
     *
     * @param blockId  ID of the block being ended
     * @param endData  contains timestamp and optional end message
     */
    void pushLogEndToBuffer(String blockId, BlockEndData endData);

    /**
     * Flush all pending blocks/logs and release resources.
     *
     * <p>Called at the end of a root block or during shutdown.
     * Implementations should ensure all buffered data is safely persisted
     * before returning.
     */
    void flush();
}
