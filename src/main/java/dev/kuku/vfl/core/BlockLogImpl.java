package dev.kuku.vfl.core;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockLogImpl implements BlockLog {
    /**
     * Buffer implementation responsible for storing and processing log entries.
     * All log data is pushed to this buffer for eventual persistence or transmission.
     */
    protected final VFLBuffer buffer;
    /**
     * The block context that this logger is associated with.
     * Contains block metadata like ID and timing information.
     */
    protected final BlockData block;
    /**
     * Thread-safe flag to ensure BLOCK_START is only logged once per block instance.
     * Uses AtomicBoolean to prevent race conditions when multiple threads attempt
     * to initialize the same block simultaneously.
     */
    protected final AtomicBoolean blockInitialized = new AtomicBoolean(false);
    /**
     * Reference to the most recently created log entry within this block.
     * Used to create a linked-list chain of logs by setting parent-child relationships.
     * Marked volatile to ensure visibility across threads when updating the chain.
     */
    protected volatile LogData currentLog;

    public BlockLogImpl(VFLBuffer buffer, BlockData block) {
        this.buffer = Objects.requireNonNull(buffer, "Buffer cannot be null");
        this.block = Objects.requireNonNull(block, "Block cannot be null");
    }

    protected String generateId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Ensures the block lifecycle begins properly by creating a BLOCK_START log entry.
     * <p>
     * The block must be initialized before any regular log entries can be created.
     * This method uses atomic compare-and-swap to guarantee that initialization
     * happens exactly once, even in multi-threaded environments.
     * <p>
     * When initialization occurs:
     * 1. Creates a BLOCK_START log entry with the block's ID as reference
     * 2. Pushes it to the buffer
     * 3. Sets it as the current log for establishing the log chain
     */
    protected void ensureBlockStarted() {
        if (blockInitialized.compareAndSet(false, true)) {
            LogData blockStartLog = createLogData(generateId(), VflLogType.BLOCK_START, null, this.block.getId());
            buffer.pushLogToBuffer(blockStartLog);
            this.currentLog = blockStartLog;
        }
    }

    /**
     * Factory method for creating LogData instances with proper parent-child relationships.
     * <p>
     * This method establishes the linked-list structure of logs by:
     * - Setting the current log's ID as the parent for the new log entry
     * - Using the current block's ID as the container
     * - Capturing the current timestamp for when the log was created
     *
     * @param id             Unique identifier for this log entry
     * @param logType        The type/severity of the log entry
     * @param message        The actual log message content
     * @param referenceBlock Optional reference to another block (used for BLOCK_START entries)
     * @return A new LogData instance ready to be pushed to the buffer
     */
    protected LogData createLogData(String id, VflLogType logType, String message, String referenceBlock) {
        String parentLogId = currentLog != null ? currentLog.getId() : null;
        return new LogData(id, this.block.getId(), parentLogId, logType, message, referenceBlock, Instant.now().toEpochMilli());
    }

    /**
     * Creates a log entry and updates the current log pointer for chaining.
     * <p>
     * This method is used for regular logging methods (text, warn, error) where
     * the new log entry becomes the "current" log in the chain. Subsequent logs
     * will reference this log as their parent, creating a sequential flow.
     * <p>
     * Flow:
     * 1. Ensures block is initialized with BLOCK_START
     * 2. Creates new log entry with current log as parent
     * 3. Pushes to buffer
     * 4. Updates currentLog reference for future chaining
     */
    protected void logAndUpdateCurrent(VflLogType logType, String message) {
        ensureBlockStarted();
        LogData logData = createLogData(generateId(), logType, message, null);
        buffer.pushLogToBuffer(logData);
        this.currentLog = logData;
    }

    /**
     * Creates a log entry without updating the current log chain pointer.
     * <p>
     * This method is used for "Here" variants (textHere, warnHere, errorHere) where
     * the log entry is created as a "side note" or "annotation" to the current point
     * in the log flow. It doesn't advance the main log chain, so subsequent regular
     * logs will still reference the previous main log as their parent.
     * <p>
     * Use case: Adding contextual information or parallel observations without
     * disrupting the main sequential flow of the log chain.
     */
    protected void logWithoutChaining(VflLogType logType, String message) {
        ensureBlockStarted();
        LogData logData = createLogData(generateId(), logType, message, null);
        buffer.pushLogToBuffer(logData);
    }

    @Override
    public void text(String message) {
        logAndUpdateCurrent(VflLogType.MESSAGE, message);
    }

    @Override
    public void textHere(String message) {
        logWithoutChaining(VflLogType.MESSAGE, message);
    }

    @Override
    public void warn(String message) {
        logAndUpdateCurrent(VflLogType.WARN, message);
    }

    @Override
    public void warnHere(String message) {
        logWithoutChaining(VflLogType.WARN, message);
    }

    @Override
    public void error(String message) {
        logAndUpdateCurrent(VflLogType.EXCEPTION, message);
    }

    @Override
    public void errorHere(String message) {
        logWithoutChaining(VflLogType.EXCEPTION, message);
    }
}