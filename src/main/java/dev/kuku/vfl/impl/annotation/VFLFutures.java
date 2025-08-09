package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Completable future wrappers for Async operations within {@link SubBlock} methods. <br>
 * IMPORTANT: Needs to be used within a block context. Calling it outside a block makes no sense as it needs to map the sub block start to the caller's block
 */
@Slf4j
public class VFLFutures {
    private static <R> Supplier<R> wrapSupplier(Supplier<R> supplier) {
        BlockContext parentContext = ThreadContextManager.GetCurrentBlockContext();
        String blockName = "Lambda block : " + Util.GetThreadInfo() + "-" + Util.TrimId(UUID.randomUUID().toString());
        //Called outside a block skip logging
        if (parentContext == null) {
            log.error("No parent context in thread {}. Supplier will be run as a normal Completable future.", Util.GetThreadInfo());
            return supplier;
        }
        var lambdaSubBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, parentContext.blockInfo.getId(), Configuration.INSTANCE.buffer);
        VFLFlowHelper.CreateLogAndPush2Buffer(parentContext.blockInfo.getId(), parentContext.currentLogId, null, lambdaSubBlock.getId(), LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, Configuration.INSTANCE.buffer);

        return () -> {
            try {
                ThreadContextManager.InitializeStackWithBlock(lambdaSubBlock);
                return supplier.get();
            } finally {
                ThreadContextManager.CloseAndPopCurrentContext(null);
                //Not necessary but safety precaution
                ThreadContextManager.CleanThreadVariables();
            }
        };
    }

    private static Runnable wrapRunnable(Runnable runnable) {
        BlockContext parentContext = ThreadContextManager.GetCurrentBlockContext();
        String blockName = "Lambda block : " + Util.GetThreadInfo() + "-" + Util.TrimId(UUID.randomUUID().toString());
        if (parentContext == null) {
            log.error("No parent context in thread {}. Runnable will be run as a normal Completable future.", Util.GetThreadInfo());
            return runnable;
        }
        var lambdaSubBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, parentContext.blockInfo.getId(), Configuration.INSTANCE.buffer);
        VFLFlowHelper.CreateLogAndPush2Buffer(parentContext.blockInfo.getId(), parentContext.currentLogId, null, lambdaSubBlock.getId(), LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, Configuration.INSTANCE.buffer);
        return () -> {
            try {
                ThreadContextManager.InitializeStackWithBlock(lambdaSubBlock);
                runnable.run();
            } finally {
                ThreadContextManager.CloseAndPopCurrentContext(null);
                //Not necessary but safety precaution
                ThreadContextManager.CleanThreadVariables();
            }
        };
    }

    // ================ PUBLIC API ================

    /**
     * invokes the supplier with the passed executor. Creates a new sub block start of type {@link LogTypeBlockStartEnum#SUB_BLOCK_START_SECONDARY_JOIN}.
     */
    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor) {

        return CompletableFuture.supplyAsync(wrapSupplier(supplier), executor);
    }

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier));
    }

    /**
     * invokes the supplier with the passed executor. Creates a new sub block start of type {@link LogTypeBlockStartEnum#SUB_BLOCK_START_SECONDARY_NO_JOIN}.
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return CompletableFuture.runAsync(wrapRunnable(runnable), executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(wrapRunnable(runnable));
    }


}
