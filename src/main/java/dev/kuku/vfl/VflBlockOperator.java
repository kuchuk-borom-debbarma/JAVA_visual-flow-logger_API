package dev.kuku.vfl;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * VflBlockOperator is the core component for creating structured, hierarchical logs in the VFL (Versioned Flow Logging) system.
 *
 * <h2>How It Works</h2>
 *
 * <h3>Block and Log Structure</h3>
 * Each VflBlockOperator represents a "block" - a logical grouping of related operations. Within each block,
 * you can create various types of logs that form a chronological chain through the {@code latestLogId} field.
 *
 * <h3>Log Chaining</h3>
 * Logs within a block are chained together:
 * <pre>
 * Block Start → Log 1 → Log 2 → Sub Block START → Sub Block END → Log 3 → Block End
 *      ↑           ↑       ↑           ↑               ↑           ↑         ↑
 *   parentId=   parentId= parentId=  parentId=     parentId=   parentId=  parentId=
 *     null      blockStart  log1      log2         subStart     log2       log3
 * </pre>
 *
 * <h3>Parent-Child Relationships</h3>
 * The system creates parent-child relationships through {@code parentLogId}:
 * <ul>
 *   <li><strong>Sequential logs</strong>: Each log's {@code parentLogId} points to the previous log (via {@code latestLogId})</li>
 *   <li><strong>Sub-block START</strong>: Points to the last log before the sub-block was created</li>
 *   <li><strong>Sub-block END</strong>: Points to the corresponding START log (NOT the latest log in the parent block)</li>
 * </ul>
 *
 * <h3>Sub-Block Lifecycle</h3>
 * When you call {@code log()} or {@code logAsync()} with a function:
 * <ol>
 *   <li>A new sub-block is created with its own {@code VflBlockOperator}</li>
 *   <li>A START log is created in the parent block, pointing to {@code latestLogId}</li>
 *   <li>{@code latestLogId} is updated to point to the START log</li>
 *   <li>The function executes within the sub-block context</li>
 *   <li>An END log is created in the parent block with {@code parentLogId = startLogId}</li>
 * </ol>
 *
 * <h3>Async Behavior</h3>
 * For {@code logAsync()}, the START log is created immediately, but the END log is created
 * asynchronously when the CompletableFuture completes. This means:
 * <ul>
 *   <li>Multiple async operations can run in parallel</li>
 *   <li>The END logs may be written out of order</li>
 *   <li>The parent-child relationship is preserved through {@code parentLogId}</li>
 *   <li>Fire-and-forget operations don't block subsequent operations</li>
 * </ul>
 *
 * <h3>Example Log Structure</h3>
 * <pre>
 * Root Block
 * ├── Log: "Operation started" (parentId: null)
 * ├── START: "Processing data" (parentId: previous log)
 * │   └── Sub Block "Data Processing"
 * │       ├── Log: "Reading file" (parentId: null in sub-block)
 * │       └── Log: "File processed" (parentId: previous log in sub-block)
 * ├── END: "Data processed successfully" (parentId: START log)
 * └── Log: "Operation completed" (parentId: END log)
 * </pre>
 *
 * <h3>Thread Safety</h3>
 * The {@code latestLogId} field should be accessed carefully in multi-threaded environments.
 * For async operations, the START log creation and {@code latestLogId} update happen
 * synchronously, ensuring proper ordering for subsequent operations.
 *
 * @author VFL Team
 * @version 1.0
 */
public class VflBlockOperator {
    private final String blockId;
    private final VflBuffer buffer;
    private String latestLogId;

    public VflBlockOperator(String blockId, VflBuffer buffer) {
        this.blockId = blockId;
        this.buffer = buffer;
        this.latestLogId = null;
    }

    public VflBlockOperator(String blockId, VflBuffer buffer, String latestLogId) {
        this.blockId = blockId;
        this.buffer = buffer;
        this.latestLogId = latestLogId;
    }

    /**
     * Add a simple message log and update the log chain.
     *
     * The new log's parentLogId will be set to the current latestLogId,
     * and latestLogId will be updated to point to this new log.
     *
     * @param message message to log
     */
    public void log(String message) {
        String logId = UUID.randomUUID().toString();
        var log = buffer.createLog(logId, this.blockId, this.latestLogId, VflLogType.MESSAGE, message, null);
        this.latestLogId = log.getId();
    }

    /**
     * Execute a function within a sub-block and create START/END logs to track its execution.
     *
     * <h3>Execution Flow:</h3>
     * <ol>
     *   <li>Create a new sub-block with unique ID</li>
     *   <li>Create START log in parent block (parentId = current latestLogId)</li>
     *   <li>Update latestLogId to point to START log</li>
     *   <li>Execute function with sub-block operator</li>
     *   <li>Create END log in parent block (parentId = START log ID)</li>
     * </ol>
     *
     * <h3>Parent-Child Linking:</h3>
     * <pre>
     * Parent Block:
     *   Previous Log (ID: A)
     *   ↓ (parentId: A)
     *   START Log (ID: B) ← latestLogId updated here
     *   ↓ (parentId: B) ← END log points back to START
     *   END Log (ID: C)
     *
     * Sub Block:
     *   Internal logs with their own chain
     * </pre>
     *
     * @param fn function to execute in the sub-block context
     * @param preBlockMessage message to log before the sub block starts
     * @param postBlockMessage function to generate message after sub block completes, receives the return value
     * @param blockName name of the sub-block for identification
     * @param <T> return type of the function
     * @return the return value from the executed function
     */
    public <T> T log(
            Function<VflBlockOperator, T> fn,
            String preBlockMessage,
            Function<T, String> postBlockMessage,
            String blockName
    ) {
        String subBlockId = UUID.randomUUID().toString();
        String startLogId = UUID.randomUUID().toString();
        //Create the block
        VflBlockOperator block = buffer.createBlock(this.blockId, subBlockId, blockName);
        //Create the log to store the starting of the sub block
        buffer.createLog(startLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(block.blockId));
        //Update latestLogId to point to startLogId
        this.latestLogId = startLogId;
        //Call the block function
        T result = fn.apply(block);
        //Add another log to store the end of the sub block with parentId set to startLogId
        String endLogId = UUID.randomUUID().toString();
        buffer.createLog(endLogId, this.blockId, startLogId,
                VflLogType.END, postBlockMessage.apply(result), Set.of(subBlockId));
        return result;
    }

    /**
     * Asynchronous version of the log method for handling non-blocking operations.
     *
     * <h3>Async Execution Flow:</h3>
     * <ol>
     *   <li>Create sub-block and START log immediately (synchronous)</li>
     *   <li>Update latestLogId to START log immediately</li>
     *   <li>Execute async function and return CompletableFuture immediately</li>
     *   <li>When CompletableFuture completes, create END log asynchronously</li>
     * </ol>
     *
     * <h3>Fire-and-Forget Behavior:</h3>
     * This method is perfect for fire-and-forget operations because:
     * <ul>
     *   <li>START log is created immediately, maintaining proper order</li>
     *   <li>Subsequent operations can continue without waiting</li>
     *   <li>END log is linked back to START log via parentLogId when async operation completes</li>
     *   <li>No blocking occurs - the CompletableFuture can be ignored</li>
     * </ul>
     *
     * <h3>Parallel Operations:</h3>
     * Multiple logAsync calls can run in parallel. Each gets its own START log immediately,
     * and their END logs are written when their respective operations complete.
     *
     * <h3>Parent-Child Linking in Async Context:</h3>
     * <pre>
     * Time 0: Previous Log (A)
     * Time 1: START Log (B) ← created immediately, parentId = A
     * Time 2: Next Operation (C) ← can start immediately, parentId = B
     * Time N: END Log (D) ← created when async completes, parentId = B (not C!)
     * </pre>
     *
     * @param fn async function to execute in the block that returns a CompletableFuture
     * @param preBlockMessage message to log before the sub block
     * @param postBlockMessage function to generate message after sub block completes
     * @param blockName name of the block
     * @param <T> return type of block
     * @return CompletableFuture with return value
     */
    public <T> CompletableFuture<T> logAsync(
            Function<VflBlockOperator, CompletableFuture<T>> fn,
            String preBlockMessage,
            Function<T, String> postBlockMessage,
            String blockName
    ) {
        String subBlockId = UUID.randomUUID().toString();
        String startLogId = UUID.randomUUID().toString();

        //Create the block
        VflBlockOperator block = buffer.createBlock(this.blockId, subBlockId, blockName);

        //Create the log to store the starting of the sub block
        buffer.createLog(startLogId, this.blockId, this.latestLogId,
                VflLogType.SUB_BLOCK_START, preBlockMessage, Set.of(block.blockId));

        //Update latestLogId to point to startLogId
        this.latestLogId = startLogId;

        //Call the async block function and handle completion
        return fn.apply(block)
                .whenComplete((result, throwable) -> {
                    //Add another log to store the end of the sub block with parentId set to startLogId
                    String endLogId = UUID.randomUUID().toString();
                    String endMessage;

                    if (throwable != null) {
                        endMessage = "Sub block failed with error: " + throwable.getMessage();
                    } else {
                        endMessage = postBlockMessage.apply(result);
                    }

                    buffer.createLog(endLogId, this.blockId, startLogId,
                            VflLogType.END, endMessage, Set.of(subBlockId));
                });
    }

    /**
     * Simplified async version that handles exceptions and provides default error messages.
     *
     * This is a convenience method that converts a static string into a function for the postBlockMessage.
     *
     * @param fn async function to execute in the block
     * @param preBlockMessage message to log before the sub block
     * @param postBlockMessage static message to log after successful execution
     * @param blockName name of the block
     * @param <T> return type of block
     * @return CompletableFuture with return value
     */
    public <T> CompletableFuture<T> logAsync(
            Function<VflBlockOperator, CompletableFuture<T>> fn,
            String preBlockMessage,
            String postBlockMessage,
            String blockName
    ) {
        return logAsync(fn, preBlockMessage, result -> postBlockMessage, blockName);
    }
}