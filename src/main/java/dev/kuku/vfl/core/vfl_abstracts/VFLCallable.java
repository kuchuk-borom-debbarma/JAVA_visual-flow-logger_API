package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum.*;


public abstract class VFLCallable extends VFL {


    private <R> R executeWithinSubBlock(String subBlockName, String subBlockStartMessage, LogTypeBlockStartEnum subBlockStartType, Supplier<R> supplier, Function<R, String> resultMessageFormatter) {
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

        // Execute the supplier with managed logging and result formatting
        return VFLFlowHelper.CallFnWithLogger(supplier, getSubBlockLogger(), resultMessageFormatter);
    }


    private <R> R executeWithExistingSubBlockContext(Supplier<R> supplier, Function<R, String> resultMessageFormatter, Block subBlock, Log subBlockStartLog) {
        // Execute the supplier with the appropriate logger and result formatting
        return VFLFlowHelper.CallFnWithLogger(supplier, getSubBlockLogger(), resultMessageFormatter);
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

    // Core async supply method - all parameters including executor
    private <R> CompletableFuture<R> supplyAsyncCore(String subBlockName, String subBlockStartMessage,
                                                     Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType,
                                                     Function<R, String> endMessageSerializer, Executor executor) {
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

        // Execute asynchronously with specified executor (null uses default)
        if (executor != null) {
            return CompletableFuture.supplyAsync(() -> {
                setupAsyncSubBlockContext(subBlock, subBlockStartLog);
                return executeWithExistingSubBlockContext(supplier, endMessageSerializer, subBlock, subBlockStartLog);
            }, executor);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                setupAsyncSubBlockContext(subBlock, subBlockStartLog);
                return executeWithExistingSubBlockContext(supplier, endMessageSerializer, subBlock, subBlockStartLog);
            });
        }
    }

    // ========== SUPPLY ASYNC - NO EXECUTOR VARIANTS ==========

    // Full method without executor
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, String subBlockStartMessage,
                                                      Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType,
                                                      Function<R, String> endMessageSerializer) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, subBlockStartType, endMessageSerializer, null);
    }

    // Minimal overload
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier) {
        return supplyAsyncCore(subBlockName, null, supplier, SUB_BLOCK_START_SECONDARY_JOIN, null, null);
    }

    // With start message
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, String subBlockStartMessage, Supplier<R> supplier) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, SUB_BLOCK_START_SECONDARY_JOIN, null, null);
    }

    // With block start type
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return supplyAsyncCore(subBlockName, null, supplier, blockStartType, null, null);
    }

    // With end message serializer
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return supplyAsyncCore(subBlockName, null, supplier, SUB_BLOCK_START_SECONDARY_JOIN, endMessageSerializer, null);
    }

    // With start message and block start type
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, String subBlockStartMessage,
                                                      Supplier<R> supplier, LogTypeBlockStartEnum blockStartType) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, blockStartType, null, null);
    }

    // With start message and end message serializer
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, String subBlockStartMessage,
                                                      Supplier<R> supplier, Function<R, String> endMessageSerializer) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, SUB_BLOCK_START_SECONDARY_JOIN, endMessageSerializer, null);
    }

    // With block start type and end message serializer
    public final <R> CompletableFuture<R> supplyAsync(String subBlockName, Supplier<R> supplier,
                                                      LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer) {
        return supplyAsyncCore(subBlockName, null, supplier, blockStartType, endMessageSerializer, null);
    }

    // ========== SUPPLY ASYNC - WITH EXECUTOR VARIANTS ==========

    // Full method with executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                          Supplier<R> supplier, LogTypeBlockStartEnum subBlockStartType,
                                                          Function<R, String> endMessageSerializer, Executor executor) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, subBlockStartType, endMessageSerializer, executor);
    }

    // Minimal with executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, Supplier<R> supplier, Executor executor) {
        return supplyAsyncCore(subBlockName, null, supplier, SUB_BLOCK_START_SECONDARY_JOIN, null, executor);
    }

    // With start message and executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                          Supplier<R> supplier, Executor executor) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, SUB_BLOCK_START_SECONDARY_JOIN, null, executor);
    }

    // With block start type and executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, Supplier<R> supplier,
                                                          LogTypeBlockStartEnum blockStartType, Executor executor) {
        return supplyAsyncCore(subBlockName, null, supplier, blockStartType, null, executor);
    }

    // With end message serializer and executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, Supplier<R> supplier,
                                                          Function<R, String> endMessageSerializer, Executor executor) {
        return supplyAsyncCore(subBlockName, null, supplier, SUB_BLOCK_START_SECONDARY_JOIN, endMessageSerializer, executor);
    }

    // With start message, block start type and executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                          Supplier<R> supplier, LogTypeBlockStartEnum blockStartType, Executor executor) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, blockStartType, null, executor);
    }

    // With start message, end message serializer and executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, String subBlockStartMessage,
                                                          Supplier<R> supplier, Function<R, String> endMessageSerializer, Executor executor) {
        return supplyAsyncCore(subBlockName, subBlockStartMessage, supplier, SUB_BLOCK_START_SECONDARY_JOIN, endMessageSerializer, executor);
    }

    // With block start type, end message serializer and executor
    public final <R> CompletableFuture<R> supplyAsyncWith(String subBlockName, Supplier<R> supplier,
                                                          LogTypeBlockStartEnum blockStartType, Function<R, String> endMessageSerializer, Executor executor) {
        return supplyAsyncCore(subBlockName, null, supplier, blockStartType, endMessageSerializer, executor);
    }

    // ========== ASYNC RUN METHOD OVERLOADS ==========

    // Core async run method - all parameters including executor
    private CompletableFuture<Void> runAsyncCore(String subBlockName, String subBlockStartMessage,
                                                 Runnable runnable, LogTypeBlockStartEnum subBlockStartType,
                                                 Executor executor) {
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

        // Execute asynchronously with specified executor (null uses default)
        if (executor != null) {
            return CompletableFuture.runAsync(() -> {
                setupAsyncSubBlockContext(subBlock, subBlockStartLog);
                executeWithExistingSubBlockContext(() -> {
                    runnable.run();
                    return null;
                }, null, subBlock, subBlockStartLog);
            }, executor);
        } else {
            return CompletableFuture.runAsync(() -> {
                setupAsyncSubBlockContext(subBlock, subBlockStartLog);
                executeWithExistingSubBlockContext(() -> {
                    runnable.run();
                    return null;
                }, null, subBlock, subBlockStartLog);
            });
        }
    }

    // ========== RUN ASYNC - NO EXECUTOR VARIANTS ==========

    // Full method without executor
    public final CompletableFuture<Void> runAsync(String subBlockName, String subBlockStartMessage,
                                                  Runnable runnable, LogTypeBlockStartEnum subBlockStartType) {
        return runAsyncCore(subBlockName, subBlockStartMessage, runnable, subBlockStartType, null);
    }

    // Minimal overload
    public final CompletableFuture<Void> runAsync(String subBlockName, Runnable runnable) {
        return runAsyncCore(subBlockName, null, runnable, SUB_BLOCK_START_SECONDARY_NO_JOIN, null);
    }

    // With start message
    public final CompletableFuture<Void> runAsync(String subBlockName, String subBlockStartMessage, Runnable runnable) {
        return runAsyncCore(subBlockName, subBlockStartMessage, runnable, SUB_BLOCK_START_SECONDARY_NO_JOIN, null);
    }

    // With block start type
    public final CompletableFuture<Void> runAsync(String subBlockName, Runnable runnable, LogTypeBlockStartEnum blockStartType) {
        return runAsyncCore(subBlockName, null, runnable, blockStartType, null);
    }

    // ========== RUN ASYNC - WITH EXECUTOR VARIANTS ==========

    // Full method with executor
    public final CompletableFuture<Void> runAsyncWith(String subBlockName, String subBlockStartMessage,
                                                      Runnable runnable, LogTypeBlockStartEnum subBlockStartType,
                                                      Executor executor) {
        return runAsyncCore(subBlockName, subBlockStartMessage, runnable, subBlockStartType, executor);
    }

    // Minimal with executor
    public final CompletableFuture<Void> runAsyncWith(String subBlockName, Runnable runnable, Executor executor) {
        return runAsyncCore(subBlockName, null, runnable, SUB_BLOCK_START_SECONDARY_NO_JOIN, executor);
    }

    // With start message and executor
    public final CompletableFuture<Void> runAsyncWith(String subBlockName, String subBlockStartMessage,
                                                      Runnable runnable, Executor executor) {
        return runAsyncCore(subBlockName, subBlockStartMessage, runnable, SUB_BLOCK_START_SECONDARY_NO_JOIN, executor);
    }

    // With block start type and executor
    public final CompletableFuture<Void> runAsyncWith(String subBlockName, Runnable runnable,
                                                      LogTypeBlockStartEnum blockStartType, Executor executor) {
        return runAsyncCore(subBlockName, null, runnable, blockStartType, executor);
    }

    // ========== EVENT PUBLISHER METHODS ==========

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
     * used by the VFLFlowHelper to manage logging during supplier/runnable execution.
     *
     * @return VFLCallable instance configured as the appropriate logger for sub-block operations
     */
    protected abstract VFLCallable getSubBlockLogger();

    /**
     * Performs implementation-specific initialization after a sub-block and its start log
     * have be  en created and registered in the execution buffer.
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