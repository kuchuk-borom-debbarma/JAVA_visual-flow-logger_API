package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.GetThreadInfo;

@Slf4j
public class VFLFutures {

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor) {
        SpawnedThreadContext spawnedThreadContext = new SpawnedThreadContext(
                ContextManager.getCurrentContext(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN
        );

        return CompletableFuture.supplyAsync(() -> {
            var newThreadCtx = ContextManager.spawnedThreadContext;
            if (newThreadCtx.get() != null) {
                log.warn("Spawned Thread Context for executor is not null! {}", GetThreadInfo());
            }
            ContextManager.spawnedThreadContext.set(spawnedThreadContext);
            return supplier.get();
        }, executor);
    }

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        SpawnedThreadContext spawnedThreadContext = new SpawnedThreadContext(
                ContextManager.getCurrentContext(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN
        );

        return CompletableFuture.supplyAsync(() -> {
            var newThreadCtx = ContextManager.spawnedThreadContext;
            if (newThreadCtx.get() != null) {
                log.warn("Spawned Thread Context for default executor is not null! {}", GetThreadInfo());
            }
            ContextManager.spawnedThreadContext.set(spawnedThreadContext);
            return supplier.get();
        });
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        SpawnedThreadContext spawnedThreadContext = new SpawnedThreadContext(
                ContextManager.getCurrentContext(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN
        );

        return CompletableFuture.runAsync(() -> {
            var newThreadCtx = ContextManager.spawnedThreadContext;
            if (newThreadCtx.get() != null) {
                log.warn("Spawned Thread Context for executor is not null! {}", GetThreadInfo());
            }
            ContextManager.spawnedThreadContext.set(spawnedThreadContext);
            runnable.run();
        }, executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        SpawnedThreadContext spawnedThreadContext = new SpawnedThreadContext(
                ContextManager.getCurrentContext(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN
        );

        return CompletableFuture.runAsync(() -> {
            var newThreadCtx = ContextManager.spawnedThreadContext;
            if (newThreadCtx.get() != null) {
                log.warn("Spawned Thread Context for default executor is not null! {}", GetThreadInfo());
            }
            ContextManager.spawnedThreadContext.set(spawnedThreadContext);
            runnable.run();
        });
    }
}