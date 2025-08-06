package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.GetThreadInfo;

@Slf4j
public class VFLFutures {
    private static SpawnedThreadContext createSpawnedThreadContext() {
        return new SpawnedThreadContext(
                ContextManager.getCurrentContext(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN
        );
    }

    /**
     * Common logic for setting up spawned thread context
     */
    private static void setupSpawnedThreadContext(SpawnedThreadContext spawnedThreadContext) {
        if (!VFLAnnotationProcessor.initialized) return;

        log.debug("Spawned thread context: {}-{}", spawnedThreadContext.parentContext().blockInfo.getBlockName(), Util.TrimId(spawnedThreadContext.parentContext().blockInfo.getId()));

        var existingCtx = ContextManager.spawnedThreadContext.get();
        if (existingCtx != null) {
            log.warn("Spawned Thread Context is not null! {}", GetThreadInfo());
        }
        ContextManager.spawnedThreadContext.set(spawnedThreadContext);
    }

    /**
     * Wraps a supplier with VFL context setup
     */
    private static <R> Supplier<R> wrapSupplier(Supplier<R> supplier) {
        return () -> {
            setupSpawnedThreadContext(createSpawnedThreadContext());
            return supplier.get();
        };
    }

    /**
     * Wraps a runnable with VFL context setup
     */
    private static Runnable wrapRunnable(Runnable runnable) {
        return () -> {
            setupSpawnedThreadContext(createSpawnedThreadContext());
            runnable.run();
        };
    }

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier), executor);
    }

    // ================ PUBLIC API ================

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        return CompletableFuture.supplyAsync(wrapSupplier(supplier));
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return CompletableFuture.runAsync(wrapRunnable(runnable), executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(wrapRunnable(runnable));
    }


}
