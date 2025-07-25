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
//TODO create a function that returns back a block context that can be passed to event publisher, Event listener can then use it to construct logger and use it. Using it will only allow creating sub block as secondary non joins. Eveery listener will be an secondary non join to it
public abstract class VFLCallable extends VFL {

    private <R> R callHandler(String blockName, String startMessage, Callable<R> callable,
                              Function<R, String> endMessageSerializer, LogTypeBlcokStartEnum logType) {
        var context = getContext();
        ensureBlockStarted();

        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, context.currentLogId, context.buffer);
        SubBlockStartLog log = VFLHelper.CreateLogAndPush2Buffer(context.blockInfo.getId(), context.currentLogId,
                startMessage, subBlock.getId(), logType, context.buffer);
        afterSubBlockAndLogCreatedAndPushed2Buffer(subBlock, log, logType);

        if (logType != SUB_BLOCK_START_PRIMARY) context.currentLogId = log.getId();
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }

    private <R> Supplier<R> createBlockSupplier(String blockName, String startMessage, Callable<R> callable,
                                                Function<R, String> endMessageSerializer, LogTypeBlcokStartEnum logType) {
        return () -> callHandler(blockName, startMessage, callable, endMessageSerializer, logType);
    }

    public final <R> R callPrimarySubBlock(String blockName, String startMessage, Callable<R> callable,
                                           Function<R, String> endMessageSerializer) {
        return createBlockSupplier(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_PRIMARY).get();
    }

    public final <R> CompletableFuture<R> callSecondaryJoiningBlock(String blockName, String startMessage,
                                                                    Callable<R> callable, Function<R, String> endMessageSerializer,
                                                                    Executor executor) {
        return CompletableFuture.supplyAsync(
                createBlockSupplier(blockName, startMessage, callable, endMessageSerializer, SUB_BLOCK_START_SECONDARY_JOIN),
                executor);
    }

    public final CompletableFuture<Void> callSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                      Runnable runnable,
                                                                      Executor executor) {
        return CompletableFuture.runAsync(() -> createBlockSupplier(blockName, startMessage, () -> {
            runnable.run();
            return null;
        }, null, SUB_BLOCK_START_SECONDARY_NO_JOIN).get(), executor);
    }

    public abstract VFLCallable getLogger();

    protected abstract void afterSubBlockAndLogCreatedAndPushed2Buffer(Block createdSubBlock, SubBlockStartLog createdSubBlockStartLog, LogTypeBlcokStartEnum startType);
}