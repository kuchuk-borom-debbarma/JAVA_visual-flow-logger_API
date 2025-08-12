package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * VFL‑enabled {@link CompletableFuture} helpers for asynchronous operations.
 *
 * <p>This utility allows you to run async tasks **while preserving the current VFL trace context**.
 * It automatically creates a secondary {@code @SubBlock} for the async task so it appears in your flow logs.
 *
 * <p><b>Key points:</b>
 * <ul>
 *   <li>Must be called <b>inside</b> an active VFL block (e.g. inside a method already started by {@link VFLStarter})</li>
 *   <li>If called outside a block, the task still runs but without VFL logging (and logs a warning)</li>
 *   <li>{@link #supplyAsync} methods → logs as <b>JOIN</b> blocks ({@link LogTypeBlockStartEnum#SUB_BLOCK_START_SECONDARY_JOIN})</li>
 *   <li>{@link #runAsync} methods → logs as <b>NO_JOIN</b> blocks ({@link LogTypeBlockStartEnum#SUB_BLOCK_START_SECONDARY_NO_JOIN})</li>
 * </ul>
 *
 * <p><b>Example:</b>
 * <pre>{@code
 * VFLStarter.StartRootBlock("ProcessOrder", () -> {
 *     CompletableFuture<String> future =
 *         VFLFutures.supplyAsync(() -> fetchOrderDetails(orderId), executor);
 *
 *     // Other synchronous code...
 *
 *     return future.join(); // Wait for async work
 * });
 * }</pre>
 *
 * <p>Using these helpers ensures that async work performed via {@code CompletableFuture}
 * is correctly linked in the VFL trace, making async flows visible in your logs.
 */
@Slf4j
public class VFLFutures {

    // Wraps a Supplier to run inside a VFL secondary JOIN sub-block
    private static <R> Supplier<R> wrapSupplier(Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier;
        }

        BlockContext parentContext = ThreadContextManager.GetCurrentBlockContext();
        if (parentContext == null) {
            log.error("No parent context in thread {}. Supplier will be run as a normal CompletableFuture.",
                    VFLHelper.GetThreadInfo());
            return supplier;
        }

        return () -> {
            try {
                String blockName = "Lambda_JOIN block : " + VFLHelper.GetThreadInfo() + "-" +
                        VFLHelper.TrimId(UUID.randomUUID().toString());
                var lambdaSubBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                        blockName,
                        parentContext.blockInfo.getId(),
                        VFLInitializer.VFLAnnotationConfig.buffer
                );
                VFLFlowHelper.CreateLogAndPush2Buffer(
                        parentContext.blockInfo.getId(),
                        parentContext.currentLogId,
                        null,
                        lambdaSubBlock.getId(),
                        LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN,
                        VFLInitializer.VFLAnnotationConfig.buffer
                );
                ThreadContextManager.PushBlockToThreadLogStack(lambdaSubBlock);

                return supplier.get();
            } finally {
                ThreadContextManager.PopCurrentStack(null);
            }
        };
    }

    // Wraps a Runnable to run inside a VFL secondary NO_JOIN sub-block
    private static Runnable wrapRunnable(Runnable runnable) {
        if (VFLInitializer.isDisabled()) {
            return runnable;
        }

        BlockContext parentContext = ThreadContextManager.GetCurrentBlockContext();
        if (parentContext == null) {
            log.error("No parent context in thread {}. Runnable will be run as a normal CompletableFuture.",
                    VFLHelper.GetThreadInfo());
            return runnable;
        }

        return () -> {
            try {
                String blockName = "Lambda_NO_JOIN block : " + VFLHelper.GetThreadInfo() + "-" +
                        VFLHelper.TrimId(UUID.randomUUID().toString());
                var lambdaSubBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                        blockName,
                        parentContext.blockInfo.getId(),
                        VFLInitializer.VFLAnnotationConfig.buffer
                );
                VFLFlowHelper.CreateLogAndPush2Buffer(
                        parentContext.blockInfo.getId(),
                        parentContext.currentLogId,
                        null,
                        lambdaSubBlock.getId(),
                        LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN,
                        VFLInitializer.VFLAnnotationConfig.buffer
                );
                ThreadContextManager.PushBlockToThreadLogStack(lambdaSubBlock);

                runnable.run();
            } finally {
                ThreadContextManager.PopCurrentStack(null);
            }
        };
    }

    // ========= PUBLIC API =========

    /**
     * Runs a supplier asynchronously using the given executor, creating a
     * secondary JOIN sub-block for logging.
     */
    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier), executor);
    }

    /**
     * Runs a supplier asynchronously (common pool), creating a
     * secondary JOIN sub-block for logging.
     */
    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier));
    }

    /**
     * Runs a runnable asynchronously using the given executor, creating a
     * secondary NO_JOIN sub-block for logging.
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return CompletableFuture.runAsync(wrapRunnable(runnable), executor);
    }

    /**
     * Runs a runnable asynchronously (common pool), creating a
     * secondary NO_JOIN sub-block for logging.
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(wrapRunnable(runnable));
    }
}
