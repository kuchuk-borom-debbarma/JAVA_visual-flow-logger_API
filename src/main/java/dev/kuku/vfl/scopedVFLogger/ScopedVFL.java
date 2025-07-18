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
//TODO_OLD support for multi thread environment by explicitly passing context. DONE! we created async version of the methods and pass the conext to the thread where it will be running
//TODO throw exceptions for fn calls but handle it gracefully within the logger too
//TODO_OLD use synchoroinse? research and think properly before deciding this ANSWER = NO
//TODO fluent api to reduce verbosity and to get rid of overloaded functions