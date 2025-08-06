package dev.kuku.vfl.impl.threadlocal.annotations;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.logger.ThreadVFL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static dev.kuku.vfl.impl.threadlocal.annotations.ThreadVFLAdviceData.parentThreadLoggerData;

public class ThreadVFLCompletableFuture {

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        // Safely get logger context or null if not available
        SpawnedThreadContext spawnedThreadData = getLoggerContextSafely(LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN);

        return CompletableFuture.supplyAsync(() -> {
            if (spawnedThreadData != null) {
                parentThreadLoggerData.set(spawnedThreadData);
            }
            return supplier.get();
        });
    }

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier, Executor executor) {
        // Safely get logger context or null if not available
        SpawnedThreadContext spawnedThreadData = getLoggerContextSafely(LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN);

        return CompletableFuture.supplyAsync(() -> {
            if (spawnedThreadData != null) {
                parentThreadLoggerData.set(spawnedThreadData);
            }
            return supplier.get();
        }, executor);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        // Safely get logger context or null if not available
        SpawnedThreadContext spawnedThreadData = getLoggerContextSafely(LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN);

        return CompletableFuture.runAsync(() -> {
            if (spawnedThreadData != null) {
                parentThreadLoggerData.set(spawnedThreadData);
            }
            runnable.run();
        });
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        // Safely get logger context or null if not available
        SpawnedThreadContext spawnedThreadData = getLoggerContextSafely(LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN);

        return CompletableFuture.runAsync(() -> {
            if (spawnedThreadData != null) {
                parentThreadLoggerData.set(spawnedThreadData);
            }
            runnable.run();
        }, executor);
    }

    /**
     * Safely captures the current logger context, returning null if no logger is available.
     * This handles cases where:
     * - No logger stack is initialized for current thread
     * - Logger stack is empty
     * - Logger is null
     * - Logger context is null
     */
    private static SpawnedThreadContext getLoggerContextSafely(LogTypeBlockStartEnum logType) {
        try {
            ThreadVFL callerLogger = ThreadVFL.getCurrentLogger();
            if (callerLogger == null) {
                return null;
            }

            VFLBlockContext parentBlock = callerLogger.loggerContext;
            if (parentBlock == null) {
                return null;
            }

            return new SpawnedThreadContext(parentBlock, logType);
        } catch (Exception e) {
            // Any exception (IllegalStateException, NullPointerException, etc.)
            // means no logger is available - return null to skip VFL setup
            return null;
        }
    }
}