package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum.*;

/**
 * Abstract class for VFL that can process callables.
 */
public abstract class VFLCallable extends VFL {

    private <R> R callHandler(String blockName, String startMessage, Callable<R> callable,
                              Function<R, String> endMessageSerializer, LogTypeBlockStartEnum logType) {
        var context = getContext();
        ensureBlockStarted();

        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, context.currentLogId, context.buffer);
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                startMessage, subBlock.getId(), logType, context.buffer);
        afterSubBlockAndLogCreatedAndPushed2Buffer(subBlock, log, logType);
        if (logType == SUB_BLOCK_START_PRIMARY) {
            context.currentLogId = log.getId();
        }
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }

    private <R> Supplier<R> createBlockSupplier(String blockName, String startMessage, Callable<R> callable,
                                                Function<R, String> endMessageSerializer, LogTypeBlockStartEnum logType) {
        return () -> callHandler(blockName, startMessage, callable, endMessageSerializer, logType);
    }

    /**
     * Start a primary sub block
     */
    public final <R> R callPrimarySubBlock(String blockName, String startMessage, Callable<R> callable,
                                           Function<R, String> endMessageSerializer) {
        return createBlockSupplier(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_PRIMARY).get();
    }

    /**
     * Create a secondary sub block. This block will join back to main flow once operation is complete.
     * If executor is null, uses ForkJoinPool.commonPool() as default.
     */
    public final <R> CompletableFuture<R> callSecondaryJoiningBlock(String blockName, String startMessage,
                                                                    Callable<R> callable, Function<R, String> endMessageSerializer,
                                                                    Executor executor) {
        Executor actualExecutor = executor != null ? executor : ForkJoinPool.commonPool();
        return CompletableFuture.supplyAsync(
                createBlockSupplier(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_SECONDARY_JOIN),
                actualExecutor);
    }

    /**
     * Create a secondary sub block. This block will not join back to the main flow.
     * If executor is null, uses ForkJoinPool.commonPool() as default.
     */
    public final CompletableFuture<Void> callSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                      Runnable runnable,
                                                                      Executor executor) {
        Executor actualExecutor = executor != null ? executor : ForkJoinPool.commonPool();
        return CompletableFuture.runAsync(() -> createBlockSupplier(blockName, startMessage, () -> {
            runnable.run();
            return null;
        }, null, SUB_BLOCK_START_SECONDARY_NO_JOIN).get(), actualExecutor);
    }

    /**
     * Overloaded method that uses default executor when none is provided
     */
    public final <R> CompletableFuture<R> callSecondaryJoiningBlock(String blockName, String startMessage,
                                                                    Callable<R> callable, Function<R, String> endMessageSerializer) {
        return callSecondaryJoiningBlock(blockName, startMessage, callable, endMessageSerializer, null);
    }

    /**
     * Overloaded method that uses default executor when none is provided
     */
    public final CompletableFuture<Void> callSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                      Runnable runnable) {
        return callSecondaryNonJoiningBlock(blockName, startMessage, runnable, null);
    }

    /**
     * Setup an event publisher and return it's data. It is added as part of the main flow. <br>
     * This event block data needs to be used by event listener for starting an event
     */
    public final EventPublisherBlock createEventPublisherBlock(String branchName, String startMessage) {
        var context = getContext();
        ensureBlockStarted();
        //Create event publisher block
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(branchName, context.currentLogId, context.buffer);
        //Create log in current logger about publishing event
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(
                context.blockInfo.getId(),
                context.currentLogId,
                startMessage,
                subBlock.getId(),
                PUBLISH_EVENT,
                context.buffer);
        getContext().currentLogId = log.getId();
        return new EventPublisherBlock(subBlock);
    }

    protected abstract VFLCallable getLogger();

    protected abstract void afterSubBlockAndLogCreatedAndPushed2Buffer(Block createdSubBlock, SubBlockStartLog createdSubBlockStartLog, LogTypeBlockStartEnum startType);
}