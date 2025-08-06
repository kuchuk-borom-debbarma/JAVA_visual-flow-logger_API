package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum.*;

/**
 * Abstract base class for VFL implementations that support executing function operations
 * within managed sub-blocks with proper logging and context management.
 * <p>
 * This class provides a framework for running synchronous and asynchronous function operations
 * where the functions receive a VFLFn logger instance, maintaining execution context,
 * logging hierarchy, and sub-block lifecycle.
 */
public abstract class VFLFn extends VFL {

    /**
     * Core execution method that orchestrates sub-block creation, logging setup,
     * and function execution with proper context management.
     * <p>
     * This method handles the complete lifecycle:
     * 1. Creates and registers a new sub-block
     * 2. Generates appropriate start logging
     * 3. Updates execution context if needed
     * 4. Delegates to implementation-specific initialization
     * 5. Executes the function with managed logging, providing the logger instance
     *
     * @param <R>                    the return type of the function
     * @param subBlockName           descriptive name for the sub-block being created
     * @param subBlockStartMessage   optional message logged when sub-block starts
     * @param subBlockStartType      determines logging behavior and execution flow control
     * @param function               the function to execute within the sub-block context, receives VFLFn logger
     * @param resultMessageFormatter optional function to format the result for logging
     * @return the result returned by the function
     */
    private <R> R executeWithinSubBlock(String subBlockName, String subBlockStartMessage, LogTypeBlockStartEnum subBlockStartType, Function<VFLFn, R> function, Function<R, String> resultMessageFormatter) {
        ensureBlockStarted();

        var executionContext = getContext();

        // Create sub-block and register it in the execution buffer
        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(subBlockName, executionContext.currentLogId, executionContext.buffer);

        // Create and register sub-block start log entry
        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
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

        // Execute the function with managed logging and result formatting
        return VFLFlowHelper.CallFnWithLogger(() -> function.apply(getSubBlockLogger(subBlock, subBlockStartLog)), getSubBlockLogger(subBlock, subBlockStartLog), resultMessageFormatter);
    }

    /**
     * Simplified execution engine for async operations where the sub-block context
     * has already been established.
     * <p>
     * This method is used internally by async methods to execute functions without
     * repeating the sub-block setup that was already done synchronously.
     *
     * @param <R>                    the return type of the function
     * @param function               the function to execute, receives VFLFn logger
     * @param resultMessageFormatter optional function to format the result for logging
     * @param subBlock               the sub-block that was already created
     * @param subBlockStartLog       the start log that was already created
     * @return the result returned by the function
     */
    private <R> R executeWithExistingSubBlockContext(Function<VFLFn, R> function, Function<R, String> resultMessageFormatter, Block subBlock, SubBlockStartLog subBlockStartLog) {
        // Execute the function with the appropriate logger and result formatting
        return VFLFlowHelper.CallFnWithLogger(() -> function.apply(getSubBlockLogger(subBlock, subBlockStartLog)), getSubBlockLogger(subBlock, subBlockStartLog), resultMessageFormatter);
    }

    // ========== APPLY METHOD OVERLOADS ==========

    // Base method with all parameters
    public final <R> R apply(String subBlockName, String subBlockStartMessage, Function<VFLFn, R> function, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, blockStartType, function, endMessageSerializer);
    }

    // Minimal overload - only mandatory parameters
    public final <R> R apply(String subBlockName, Function<VFLFn, R> function) {
        return executeWithinSubBlock(subBlockName, null, SUB_BLOCK_START_PRIMARY, function, null);
    }

    // With start message
    public final <R> R apply(String subBlockName, String subBlockStartMessage, Function<VFLFn, R> function) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, SUB_BLOCK_START_PRIMARY, function, null);
    }

    // With block start type
    public final <R> R apply(String subBlockName, Function<VFLFn, R> function, LogTypeBlockStartEnum blockStartType) {
        return executeWithinSubBlock(subBlockName, null, blockStartType, function, null);
    }

    // With end message serializer
    public final <R> R apply(String subBlockName, Function<VFLFn, R> function, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, null, SUB_BLOCK_START_PRIMARY, function, endMessageSerializer);
    }

    // With start message and block start type
    public final <R> R apply(String subBlockName, String subBlockStartMessage, Function<VFLFn, R> function, LogTypeBlockStartEnum blockStartType) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, blockStartType, function, null);
    }

    // With start message and end message serializer
    public final <R> R apply(String subBlockName, String subBlockStartMessage, Function<VFLFn, R> function, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, subBlockStartMessage, SUB_BLOCK_START_PRIMARY, function, endMessageSerializer);
    }

    // With block start type and end message serializer
    public final <R> R apply(String subBlockName, Function<VFLFn, R> function, LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return executeWithinSubBlock(subBlockName, null, blockStartType, function, endMessageSerializer);
    }

    // ========== ACCEPT METHOD OVERLOADS ==========

    // Base accept method with all parameters
    public final void accept(String subBlockName, String subBlockStartMessage, Consumer<VFLFn> consumer, LogTypeBlockStartEnum blockStartType) {
        executeWithinSubBlock(subBlockName, subBlockStartMessage, blockStartType, (vflFn) -> {
            consumer.accept(vflFn);
            return null;
        }, null);
    }

    // Minimal accept overload
    public final void accept(String subBlockName, Consumer<VFLFn> consumer) {
        accept(subBlockName, null, consumer, SUB_BLOCK_START_PRIMARY);
    }

    // With start message
    public final void accept(String subBlockName, String subBlockStartMessage, Consumer<VFLFn> consumer) {
        accept(subBlockName, subBlockStartMessage, consumer, SUB_BLOCK_START_PRIMARY);
    }

    // With block start type
    public final void accept(String subBlockName, Consumer<VFLFn> consumer, LogTypeBlockStartEnum blockStartType) {
        accept(subBlockName, null, consumer, blockStartType);
    }

    // ========== ASYNC APPLY METHOD OVERLOADS ==========

    // Base async apply method
    public final <R> CompletableFuture<R> applyAsync(String subBlockName, String subBlockStartMessage, Function<VFLFn, R> function, LogTypeBlockStartEnum subBlockStartType, Function<R, String> endMessageSerializer) {
        // In current thread: create the block, log and push to buffer
        ensureBlockStarted();
        var executionContext = getContext();

        // Create sub-block and push to buffer
        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(subBlockName, executionContext.currentLogId, executionContext.buffer);

        // Create and push sub-block start log
        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
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
            // Execute the function with existing context
            return executeWithExistingSubBlockContext(function, endMessageSerializer, subBlock, subBlockStartLog);
        });
    }

    // Minimal async apply overload
    public final <R> CompletableFuture<R> applyAsync(String subBlockName, Function<VFLFn, R> function) {
        return applyAsync(subBlockName, null, function, SUB_BLOCK_START_SECONDARY_JOIN, null);
    }

    // With start message
    public final <R> CompletableFuture<R> applyAsync(String subBlockName, String subBlockStartMessage, Function<VFLFn, R> function) {
        return applyAsync(subBlockName, subBlockStartMessage, function, SUB_BLOCK_START_SECONDARY_JOIN, null);
    }

    // With block start type
    public final <R> CompletableFuture<R> applyAsync(String subBlockName, Function<VFLFn, R> function, LogTypeBlockStartEnum blockStartType) {
        return applyAsync(subBlockName, null, function, blockStartType, null);
    }

    // With end message serializer
    public final <R> CompletableFuture<R> applyAsync(String subBlockName, Function<VFLFn, R> function, Function<R, String> endMessageSerializer) {
        return applyAsync(subBlockName, null, function, SUB_BLOCK_START_SECONDARY_JOIN, endMessageSerializer);
    }

    // ========== ASYNC ACCEPT METHOD OVERLOADS ==========

    // Base async accept method
    public final CompletableFuture<Void> acceptAsync(String subBlockName, String subBlockStartMessage, Consumer<VFLFn> consumer, LogTypeBlockStartEnum subBlockStartType) {
        // In current thread: create the block, log and push to buffer
        ensureBlockStarted();
        var executionContext = getContext();

        // Create sub-block and push to buffer
        Block subBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(subBlockName, executionContext.currentLogId, executionContext.buffer);

        // Create and push sub-block start log
        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
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

            // Execute the consumer with existing context
            executeWithExistingSubBlockContext((vflFn) -> {
                consumer.accept(vflFn);
                return null;
            }, null, subBlock, subBlockStartLog);
        });
    }

    // Minimal async accept overload
    public final CompletableFuture<Void> acceptAsync(String subBlockName, Consumer<VFLFn> consumer) {
        return acceptAsync(subBlockName, null, consumer, SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    // With start message
    public final CompletableFuture<Void> acceptAsync(String subBlockName, String subBlockStartMessage, Consumer<VFLFn> consumer) {
        return acceptAsync(subBlockName, subBlockStartMessage, consumer, SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    // With block start type
    public final CompletableFuture<Void> acceptAsync(String subBlockName, Consumer<VFLFn> consumer, LogTypeBlockStartEnum blockStartType) {
        return acceptAsync(subBlockName, null, consumer, blockStartType);
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
        Block eventPublisherSubBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(eventBranchName, executionContext.currentLogId, executionContext.buffer);

        // Create log entry documenting the event publisher creation
        SubBlockStartLog eventPublishLog = VFLFlowHelper.CreateLogAndPush2Buffer(
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
     * used by the VFLFlowHelper to manage logging during function execution and
     * will be passed as the parameter to the executed functions.
     *
     * @param subBlock         the sub-block that was created for this execution context
     * @param subBlockStartLog the start log entry associated with this sub-block
     * @return VFLFn instance configured as the appropriate logger for sub-block operations
     */
    protected abstract VFLFn getSubBlockLogger(Block subBlock, SubBlockStartLog subBlockStartLog);

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
     * This method is called on the async thread before executing the function, allowing
     * implementations to set up thread-local variables, logging context, or other thread-specific
     * state that was established in the original calling thread.
     *
     * @param subBlock         the sub-block that will execute on this async thread
     * @param subBlockStartLog the start log entry associated with this sub-block
     */
    protected abstract void setupAsyncSubBlockContext(Block subBlock, Log subBlockStartLog);
}