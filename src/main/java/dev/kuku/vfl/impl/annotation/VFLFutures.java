package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Slf4j
public class VFLFutures {
    /**
     * Wraps a supplier with VFL context setup
     */
    private static <R> Supplier<R> wrapSupplier(Supplier<R> supplier) {
        BlockContext parentContext = ThreadContextManager.GetCurrentBlockContext();
        return () -> {
            try {
                //Create spawned thread context in the executor thread
                ThreadContextManager.InitializeThreadStackWithSpawnedThreadContext(new SpawnedThreadContext(parentContext, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN));
                return supplier.get();
            } finally {
                //If user Logs inside the lambda but not within a VFL block then a lambda sub block start step is created as part of the flow which is NOT removed by context manager as CM only managers methods that are annotated with @SubBlock and needs to be removed manually.
                ThreadContextManager.CleanThreadVariables();
            }
        };
    }

    /**
     * Wraps a runnable with VFL context setup
     */
    private static Runnable wrapRunnable(Runnable runnable) {
        BlockContext parentContext = ThreadContextManager.GetCurrentBlockContext();
        return () -> {
            try {
                //Create spawned thread context in the executor thread
                ThreadContextManager.InitializeThreadStackWithSpawnedThreadContext(new SpawnedThreadContext(parentContext, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN));
                runnable.run();

            } finally {
                //If user Logs inside the lambda but not within a VFL block then a lambda sub block start step is created as part of the flow which is NOT removed by context manager as CM only managers methods that are annotated with @SubBlock and needs to be removed manually.
                ThreadContextManager.CleanThreadVariables();
            }
        };
    }
    // ================ PUBLIC API ================
    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor) {

        return CompletableFuture.supplyAsync(wrapSupplier(supplier), executor);
    }

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
