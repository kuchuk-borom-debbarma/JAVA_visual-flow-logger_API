package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum.*;

/**
 * Abstract class for VFL that can process callables.
 */
public abstract class VFLCallable extends VFL {

    private <R> R callHandler(String blockName, String startMessage, Callable<R> callable,
                              Function<R, String> endMessageSerializer, LogTypeBlcokStartEnum logType) {
        var context = getContext();
        ensureBlockStarted();

        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, context.currentLogId, context.buffer);
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                startMessage, subBlock.getId(), logType, context.buffer);
        afterSubBlockAndLogCreatedAndPushed2Buffer(subBlock, log, logType);
        if (logType != SUB_BLOCK_START_PRIMARY) {
            context.currentLogId = log.getId();
        }
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }

    private <R> Supplier<R> createBlockSupplier(String blockName, String startMessage, Callable<R> callable,
                                                Function<R, String> endMessageSerializer, LogTypeBlcokStartEnum logType) {
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
     */
    public final <R> CompletableFuture<R> callSecondaryJoiningBlock(String blockName, String startMessage,
                                                                    Callable<R> callable, Function<R, String> endMessageSerializer,
                                                                    Executor executor) {
        return CompletableFuture.supplyAsync(
                createBlockSupplier(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_SECONDARY_JOIN),
                executor);
    }

    /**
     * Create a secondary sub block. This block will not join back to the main flow.
     */
    public final CompletableFuture<Void> callSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                      Runnable runnable,
                                                                      Executor executor) {
        return CompletableFuture.runAsync(() -> createBlockSupplier(blockName, startMessage, () -> {
            runnable.run();
            return null;
        }, null, SUB_BLOCK_START_SECONDARY_NO_JOIN).get(), executor);
    }
    /**
     * Create a Primary Sub block and return it's block. This can be passed on to other service, methods which can then use it to continue the operation. The method/service receiving the block data SHOULD always start a secondary non joining sub block
     */
    public final Block createBranchingSubBlockData(String branchName, String startMessage) {
        //TODO create a new block data type specifically for branching sub block. Then create a new runner function that takes that in as a param. And then it will start a sub block respectively.
        var context = getContext();
        ensureBlockStarted();
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(branchName, context.currentLogId, context.buffer);
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                startMessage, subBlock.getId(), SUB_BLOCK_START_PRIMARY, context.buffer);
        return subBlock;
    }

    public abstract VFLCallable getLogger();

    protected abstract void afterSubBlockAndLogCreatedAndPushed2Buffer(Block createdSubBlock, SubBlockStartLog createdSubBlockStartLog, LogTypeBlcokStartEnum startType);
}