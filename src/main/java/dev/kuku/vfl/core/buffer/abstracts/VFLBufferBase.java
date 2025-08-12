package dev.kuku.vfl.core.buffer.abstracts;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.models.logs.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base implementation of {@link VFLBuffer} with in‑memory, size‑based batching.
 *
 * <p>This abstract class provides:
 * <ul>
 *     <li>Thread‑safe storage of incoming logs, blocks, and block start/end events</li>
 *     <li>Automatic flush when the total buffered item count exceeds the configured {@code bufferSize}</li>
 *     <li>A single {@link #onFlushAll(List, List, Map, Map)} callback point for subclasses
 *         to perform actual persistence or forwarding</li>
 * </ul>
 *
 * <h2>How flushing works</h2>
 * <ol>
 *   <li>Incoming push methods ({@code pushLogToBuffer}, {@code pushBlockToBuffer}, etc.)
 *       store the event in an internal collection.</li>
 *   <li>If the total number of buffered items exceeds {@code bufferSize},
 *       {@link #flushAll()} is triggered automatically.</li>
 *   <li>{@code flushAll()} copies and clears the internal collections under a lock, then
 *       calls {@link #onFlushAll(List, List, Map, Map)} without holding the lock.</li>
 *   <li>Multiple flushes can run concurrently — each gets its own copy of the data to handle.</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>All buffer mutations are guarded by a {@link ReentrantLock}.</li>
 *   <li>The {@code onFlushAll} method is called without any locks, so implementations must be
 *       thread‑safe if they mutate shared resources.</li>
 * </ul>
 *
 * <h2>Extending this class</h2>
 * To create a custom buffer, extend this class and implement {@link #onFlushAll(List, List, Map, Map)}:
 * <pre>{@code
 * public class ConsoleVFLBuffer extends VFLBufferBase {
 *     public ConsoleVFLBuffer(int bufferSize) {
 *         super(bufferSize);
 *     }
 *
 *     @Override
 *     protected void onFlushAll(List<Log> logs,
 *                               List<Block> blocks,
 *                               Map<String, Long> blockStarts,
 *                               Map<String, BlockEndData> blockEnds) {
 *         logs.forEach(l -> System.out.println("LOG: " + l));
 *         blocks.forEach(b -> System.out.println("BLOCK: " + b));
 *         blockStarts.forEach((id, ts) -> System.out.println("START: " + id + " -> " + ts));
 *         blockEnds.forEach((id, data) -> System.out.println("END: " + id + " -> " + data));
 *     }
 * }
 * }</pre>
 *
 * <h2>When to call {@link #flush()}</h2>
 * <ul>
 *   <li>Normally, flushing is automatic when the buffer is full</li>
 *   <li>You can manually call {@link #flush()} to force a flush (e.g., at shutdown)</li>
 * </ul>
 */
public abstract class VFLBufferBase implements VFLBuffer {
    private final int bufferSize;
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Log> logs2Flush;
    private final List<Block> blocks2Flush;
    private final Map<String, Long> blockStarts2Flush;
    private final Map<String, BlockEndData> blockEnds2Flush;

    /**
     * @param bufferSize maximum number of total items (logs + blocks + starts + ends)
     *                   to buffer before triggering an automatic flush
     */
    public VFLBufferBase(int bufferSize) {
        this.bufferSize = bufferSize;
        logs2Flush = new ArrayList<>();
        blocks2Flush = new ArrayList<>();
        blockStarts2Flush = new HashMap<>();
        blockEnds2Flush = new HashMap<>();
    }

    @Override
    public void pushLogToBuffer(Log log) {
        lock.lock();
        try {
            this.logs2Flush.add(log);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    @Override
    public void pushBlockToBuffer(Block block) {
        lock.lock();
        try {
            this.blocks2Flush.add(block);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    @Override
    public void pushLogStartToBuffer(String blockId, long timestamp) {
        lock.lock();
        try {
            this.blockStarts2Flush.put(blockId, timestamp);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    @Override
    public void pushLogEndToBuffer(String blockId, BlockEndData endData) {
        lock.lock();
        try {
            blockEnds2Flush.put(blockId, endData);
        } finally {
            lock.unlock();
        }
        flushIfFull();
    }

    /**
     * Checks current buffer size and triggers a flush if over limit.
     */
    private void flushIfFull() {
        boolean shouldFlush = false;
        lock.lock();
        try {
            int totalSize = logs2Flush.size()
                    + blocks2Flush.size()
                    + blockStarts2Flush.size()
                    + blockEnds2Flush.size();
            if (totalSize > bufferSize) {
                shouldFlush = true;
            }
        } finally {
            lock.unlock();
        }
        if (shouldFlush) {
            flushAll();
        }
    }

    /**
     * Copies and clears all pending entries, then calls {@link #onFlushAll(List, List, Map, Map)}.
     * <p>This design avoids holding the lock during slow IO operations in {@code onFlushAll()}.</p>
     */
    protected void flushAll() {
        List<Log> logsToFlush;
        List<Block> blocksToFlush;
        Map<String, Long> blockStartsToFlush;
        Map<String, BlockEndData> blockEndsToFlush;

        lock.lock();
        try {
            logsToFlush = new ArrayList<>(logs2Flush);
            blocksToFlush = new ArrayList<>(blocks2Flush);
            blockStartsToFlush = new HashMap<>(blockStarts2Flush);
            blockEndsToFlush = new HashMap<>(blockEnds2Flush);

            logs2Flush.clear();
            blocks2Flush.clear();
            blockStarts2Flush.clear();
            blockEnds2Flush.clear();
        } finally {
            lock.unlock(); // lock is released before the flush is processed
        }

        // Multiple flush calls can run in parallel, each with its own snapshot
        onFlushAll(logsToFlush, blocksToFlush, blockStartsToFlush, blockEndsToFlush);
    }

    @Override
    public void flush() {
        flushAll();
    }

    /**
     * Called whenever the buffer is flushed (manually or because it is full).
     *
     * <p>Subclasses must implement this method to define how and where the flushed
     * data is sent (e.g., to disk, database, queue, or logging system).</p>
     *
     * @param logs        snapshot of logs to flush
     * @param blocks      snapshot of blocks to flush
     * @param blockStarts snapshot of block start timestamps
     * @param blockEnds   snapshot of block end data
     */
    protected abstract void onFlushAll(List<Log> logs,
                                       List<Block> blocks,
                                       Map<String, Long> blockStarts,
                                       Map<String, BlockEndData> blockEnds);
}
