package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum.*;

/**
 * Abstract base class for VFL implementations that support executing callable operations
 * within managed sub-blocks with proper logging and context management.
 * <p>
 * This class provides a framework for running synchronous and asynchronous operations
 * while maintaining execution context, logging hierarchy, and sub-block lifecycle.
 */
public abstract class VFLCallable extends VFL {

    /**
     * Core execution engine that orchestrates sub-block creation, logging setup,
     * and supplier execution with proper context management.
     * <p>
     * This method handles the complete lifecycle:
     * 1. Creates and registers a new sub-block
     * 2. Generates appropriate start logging
     * 3. Updates execution context if needed
     * 4. Delegates to implementation-specific initialization
     * 5. Executes the supplier with managed logging
     *
     * @param <R>                    the return type of the supplier
     * @param subBlockName           descriptive name for the sub-block being created
     * @param subBlockStartMessage   optional message logged when sub-block starts
     * @param subBlockStartType      determines logging behavior and execution flow control
     * @param supplier               the operation to execute within the sub-block context
     * @param resultMessageFormatter optional function to format the result for logging
     * @return the result returned by the supplier
     */
    private <R> R executeWithinSubBlock(String subBlockName, String subBlockStartMessage, LogTypeBlockStartEnum subBlockStartType, Supplier<R> supplier, Function<R, String> resultMessageFormatter) {
        ensureBlockStarted();

        var executionContext = getContext();

        // Create sub-block and register it in the execution buffer
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(subBlockName, executionContext.currentLogId, executionContext.buffer);

        // Create and register sub-block start log entry
        SubBlockStartLog subBlockStartLog = VFLHelper.CreateLogAndPush2Buffer(
                executionContext.blockInfo.getId(),
                executionContext.currentLogId,
                subBlockStartMessage,
                subBlock.getId(),
                subBlockStartType,
                executionContext.buffer
        );

        // Update current log ID if this is a primary sub-block that affects execution flow
        if (subBlockStartType == SUB_BLOCK_START_PRIMARY) {
            executionContext.currentLogId = subBlockStartLog.getId();
        }

        // Delegate to implementation-specific sub-block initialization
        initializeSubBlockInImplementation(executionContext, subBlock, subBlockStartLog);

        // Execute the supplier with managed logging and result formatting
        return VFLHelper.CallFnWithLogger(supplier, getSubBlockLogger(), resultMessageFormatter);
    }

    /**
     * Simplified execution engine for async operations where the sub-block context
     * has already been established in the calling thread.
     * <p>
     * This method is used internally by async methods to execute suppliers without
     * repeating the sub-block setup that was already done synchronously.
     *
     * @param <R>                    the return type of the supplier
     * @param supplier               the operation to execute
     * @param resultMessageFormatter optional function to format the result for logging
     * @return the result returned by the supplier
     */
    private <R> R executeWithExistingSubBlockContext(Supplier<R> supplier, Function<R, String> resultMessageFormatter) {
        // Execute the supplier with the appropriate logger and result formatting
        return VFLHelper.CallFnWithLogger(supplier, getSubBlockLogger(), resultMessageFormatter);
    }

    // ========== SUPPLY METHOD OVERLOADS ==========

    // Base method with all parameters
    public final <R> R supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, blockStartType, supplier, endMessageSerializer);
    }

    // Minimal overload - only mandatory parameters
    public final <R> R supply(String subBlockName, Supplier<R> supplier) {
        return executeWithinSubBlock(subBlockName, null, SUB_BLOCK_START_PRIMARY, supplier, null);
    }

    // With start message
    public final <R> R supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, SUB_BLOCK_START_PRIMARY, supplier, null);
    }

    // With block start type
    public final <R> R supply(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return executeWithinSubBlock(subBlockName, null, blockStartType, supplier, null);
    }

    // With end message serializer
    public final <R> R supply(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, null, SUB_BLOCK_START_PRIMARY, supplier, endMessageSerializer);
    }

    // With start message and block start type
    public final <R> R supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, blockStartType, supplier, null);
    }

    // With start message and end message serializer
    public final <R> R supply(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, SUB_BLOCK_START_PRIMARY, supplier, endMessageSerializer);
    }

    // With block start type and end message serializer
    public final <R> R supply(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, null, blockStartType, supplier, endMessageSerializer);
    }

    // ========== RUN METHOD OVERLOADS ==========

    // Base run method with all parameters
    public final void run(String subBlockName, String subBlockStartMessage, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        executeWithinSubBlock(subBlockName, subBlockStartMessage, blockStartType, () -> {
            runnable.run();
            return null;
        }, null);
    }

    // Minimal run overload
    public final void run(String subBlockName, Runnable runnable) {
        run(subBlockName, null, runnable, SUB_BLOCK_START_PRIMARY);
    }

    // With start message
    public final void run(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        run(subBlockName, subBlockStartMessage, runnable, SUB_BLOCK_START_PRIMARY);
    }

    // With block start type
    public final void run(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        run(subBlockName, null, runnable, blockStartType);
    }

    // ========== ASYNC SUPPLY METHOD OVERLOADS ==========

    // Base async supply method
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType, Function<R, String> endMessageSerializer) {
        // In current thread: create the block, log and push to buffer
        ensureBlockStarted();
        var executionContext = getContext();

        // Create sub-block and push to buffer
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(subBlockName, executionContext.currentLogId, executionContext.buffer);

        // Create and push sub-block start log
        SubBlockStartLog subBlockStartLog = VFLHelper.CreateLogAndPush2Buffer(
                executionContext.blockInfo.getId(),
                executionContext.currentLogId,
                subBlockStartMessage,
                subBlock.getId(),
                subBlockStartType,
                executionContext.buffer
        );

        // Update current log ID if this is a primary sub-block
        if (subBlockStartType == SUB_BLOCK_START_PRIMARY) {
            executionContext.currentLogId = subBlockStartLog.getId();
        }

        // Execute asynchronously in new thread
        return CompletableFuture.supplyAsync(() -> {
            // In new thread: setup async sub-block context
            setupAsyncSubBlockContext(subBlock, subBlockStartLog);
            // Execute the supplier with existing context
            return executeWithExistingSubBlockContext(supplier, endMessageSerializer);
        });
    }

    // Minimal async supply overload
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier) {
        return supplyAsync(subBlockName, null, supplier, SUB_BLOCK_START_SECONDARY_JOIN, null);
    }

    // With start message
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        return supplyAsync(subBlockName, subBlockStartMessage, supplier, SUB_BLOCK_START_SECONDARY_JOIN, null);
    }

    // With block start type
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return supplyAsync(subBlockName, null, supplier, blockStartType, null);
    }

    // With end message serializer
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return supplyAsync(subBlockName, null, supplier, SUB_BLOCK_START_SECONDARY_JOIN, endMessageSerializer);
    }

    // ========== ASYNC RUN METHOD OVERLOADS ==========

    // Base async run method
    public final CompletableFuture<Void> runAsync(String subBlockName, String subBlockStartMessage, Runnable runnable, LogTypeBlockStartEnum subBlockStartType) {
        // In current thread: create the block, log and push to buffer
        ensureBlockStarted();
        var executionContext = getContext();

        // Create sub-block and push to buffer
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(subBlockName, executionContext.currentLogId, executionContext.buffer);

        // Create and push sub-block start log
        SubBlockStartLog subBlockStartLog = VFLHelper.CreateLogAndPush2Buffer(
                executionContext.blockInfo.getId(),
                executionContext.currentLogId,
                subBlockStartMessage,
                subBlock.getId(),
                subBlockStartType,
                executionContext.buffer
        );

        // Update current log ID if this is a primary sub-block
        if (subBlockStartType == SUB_BLOCK_START_PRIMARY) {
            executionContext.currentLogId = subBlockStartLog.getId();
        }

        // Execute asynchronously in new thread
        return CompletableFuture.runAsync(() -> {
            // In new thread: setup async sub-block context
            setupAsyncSubBlockContext(subBlock, subBlockStartLog);

            // Execute the runnable with existing context
            executeWithExistingSubBlockContext(() -> {
                runnable.run();
                return null;
            }, null);
        });
    }

    // Minimal async run overload
    public final CompletableFuture<Void> runAsync(String subBlockName, Runnable runnable) {
        return runAsync(subBlockName, null, runnable, SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    // With start message
    public final CompletableFuture<Void> runAsync(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        return runAsync(subBlockName, subBlockStartMessage, runnable, SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    // With block start type
    public final CompletableFuture<Void> runAsync(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        return runAsync(subBlockName, null, runnable, blockStartType);
    }

    // ========== EVENT PUBLISHER METHODS ==========

    /**
     * Creates and registers an event publisher block for asynchronous event-driven execution.
     *
     * <p>The returned {@link EventPublisherBlock} must be used with the appropriate runner methods
     * to ensure proper logger initialization and context propagation:
     * <ul>
     * <li>{@link dev.kuku.vfl.core.vfl_abstracts.runner.VFLCallableRunner#startEventListenerLogger}</li>
     * <li>{@link dev.kuku.vfl.core.vfl_abstracts.runner.VFLFnRunner#startEventListenerLogger}</li>
     * </ul>
     *
     * @param eventBranchName     descriptive identifier for the event publishing branch
     * @param publishStartMessage message logged when the event publisher is created, or null for no message
     * @return {@link EventPublisherBlock} containing the context required by event listeners
     * @see EventPublisherBlock
     */
    public final EventPublisherBlock createEventPublisherBlock(String eventBranchName, String publishStartMessage) {
        var executionContext = getContext();
        ensureBlockStarted();

        // Create event publisher sub-block and register it
        Block eventPublisherSubBlock = VFLHelper.CreateBlockAndPush2Buffer(eventBranchName, executionContext.currentLogId, executionContext.buffer);

        // Create log entry documenting the event publisher creation
        SubBlockStartLog eventPublishLog = VFLHelper.CreateLogAndPush2Buffer(
                executionContext.blockInfo.getId(),
                executionContext.currentLogId,
                publishStartMessage,
                eventPublisherSubBlock.getId(),
                PUBLISH_EVENT,
                executionContext.buffer
        );

        getContext().currentLogId = eventPublishLog.getId();
        return new EventPublisherBlock(eventPublisherSubBlock);
    }

    // ========== ABSTRACT METHODS ==========

    /**
     * Provides the logger instance that should be used for sub-block execution.
     * <p>
     * Implementations must return a logger that is properly configured for the
     * current execution context and sub-block hierarchy. This logger will be
     * used by the VFLHelper to manage logging during supplier/runnable execution.
     *
     * @return VFLCallable instance configured as the appropriate logger for sub-block operations
     */
    protected abstract VFLCallable getSubBlockLogger();

    /**
     * Performs implementation-specific initialization after a sub-block and its start log
     * have been created and registered in the execution buffer.
     * <p>
     * This hook allows implementations to set up any necessary context, logging infrastructure,
     * or thread-local state required for proper sub-block execution. For example, ThreadVFL
     * uses this to push a new logger instance onto its ThreadLocal logger stack.
     *
     * @param executionContext the current execution context when sub-block initialization was triggered
     * @param createdSubBlock  the sub-block that was created and registered
     * @param subBlockStartLog the start log entry that was created for this sub-block
     */
    protected abstract void initializeSubBlockInImplementation(VFLBlockContext executionContext, Block createdSubBlock, SubBlockStartLog subBlockStartLog);

    /**
     * Establishes the necessary execution context for a sub-block running on an asynchronous thread.
     * <p>
     * This method is called on the async thread before executing the supplier/runnable, allowing
     * implementations to set up thread-local variables, logging context, or other thread-specific
     * state that was established in the original calling thread.
     *
     * @param subBlock         the sub-block that will execute on this async thread
     * @param subBlockStartLog the start log entry associated with this sub-block
     */
    protected abstract void setupAsyncSubBlockContext(Block subBlock, Log subBlockStartLog);
}