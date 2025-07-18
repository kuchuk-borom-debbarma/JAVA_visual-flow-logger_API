package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.VFL;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Scoped logger interface for use with ScopedValue.
 */
public interface ScopedVFL extends VFL {
    void run(String blockName, String message, Runnable runnable);

    /**
     * Run a task as completable future. Optinoally can take the executor. <br>
     * VFL creates sub logger context in the running thread. <br>
     * Please note that the log chain will NOT move forward as it is an async operation.
     */
    CompletableFuture<Void> runAsync(String blockName, String message, Runnable runnable);

    CompletableFuture<Void> runAsync(String blockName, String message, Runnable runnable, Executor executor);

    void runHere(String blockName, String message, Runnable runnable);

    <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> CompletableFuture<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> CompletableFuture<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable, Executor executor);

    <T> T call(String blockName, String message, Callable<T> callable);

    <T> CompletableFuture<T> callAsync(String blockName, String message, Callable<T> callable);

    <T> CompletableFuture<T> callAsync(String blockName, String message, Callable<T> callable, Executor executor);

    <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T callHere(String blockName, String message, Callable<T> callable);

}
//TODO support for multi thread environment by explicitly passing context
//TODO throw exceptions for fn calls but handle it gracefully within the logger too
//TODO use synchoroinse? research and think properly before deciding this