package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum.*;

/**
 * Abstract class for VFL that can process callables.
 */
public abstract class VFLCallable extends VFL {

    /**
     * Start a primary sub block
     */
    public final <R> R callPrimarySubBlock(String blockName, String startMessage, Callable<R> callable,
                                           Function<R, String> endMessageSerializer) {
        var context = getContext();
        ensureBlockStarted();
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, context.currentLogId, context.buffer);
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                startMessage, subBlock.getId(), SUB_BLOCK_START_PRIMARY, context.buffer);
        context.currentLogId = log.getId();
        prepareLoggerAfterSubBlockStartDataInitializedAndPushed(context, subBlock, log, SUB_BLOCK_START_PRIMARY);
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }

    /**
     * Create a secondary sub block. This block will join back to main flow once operation is complete.
     */
    public final <R> CompletableFuture<R> callSecondaryJoiningBlock(String blockName, String startMessage,
                                                                    Callable<R> callable, Function<R, String> endMessageSerializer,
                                                                    Executor executor) {
        var context = getContext();
        ensureBlockStarted();
        //This will be called in the executor thread
        Supplier<R> c = () -> {
            Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, context.currentLogId, context.buffer);
            SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                    startMessage, subBlock.getId(), SUB_BLOCK_START_SECONDARY_JOIN, context.buffer);
            prepareLoggerAfterSubBlockStartDataInitializedAndPushed(context, subBlock, log, SUB_BLOCK_START_SECONDARY_JOIN);
            return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
        };
        if (executor != null)
            return CompletableFuture.supplyAsync(
                    c,
                    executor);
        return CompletableFuture.supplyAsync(
                c);
    }

    /**
     * Create a secondary sub block. This block will not join back to the main flow.
     */
    public final CompletableFuture<Void> callSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                      Runnable runnable,
                                                                      Executor executor) {
        var context = getContext();
        ensureBlockStarted();
        //This will be called in the executor thread
        Runnable r = () -> {
            Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, context.currentLogId, context.buffer);
            SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                    startMessage, subBlock.getId(), SUB_BLOCK_START_SECONDARY_JOIN, context.buffer);
            prepareLoggerAfterSubBlockStartDataInitializedAndPushed(context, subBlock, log, SUB_BLOCK_START_SECONDARY_NO_JOIN);
            VFLHelper.CallFnWithLogger(() -> {
                runnable.run();
                return null;
            }, getLogger(), null);
        };
        if (executor != null)
            return CompletableFuture.runAsync(r, executor);
        return CompletableFuture.runAsync(r);
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

    /**
     * This method is invoked after pushing the sub block start log & sub block data to the buffer. <br>
     * It's main purpose is to setup the logger to handle the sub block start.
     * <br>
     * {@link dev.kuku.vfl.ThreadVFL}
     * In the Child class above, It is used to add a new logger instance to the ThreadLocal logger stack.
     *
     * @param parentBlockCtx   context under which the sub block start operation was invoked
     * @param subBlock         The subblock data that was created and pushed to buffer
     * @param subBlockStartLog The subblock start log that was created and pushed to buffer
     * @param logType          The type of subblock start.
     */
    protected abstract void prepareLoggerAfterSubBlockStartDataInitializedAndPushed(VFLBlockContext parentBlockCtx, Block subBlock, SubBlockStartLog subBlockStartLog, LogTypeBlockStartEnum logType);
}
