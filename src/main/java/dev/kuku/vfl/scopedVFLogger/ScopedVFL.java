package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.VFL;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Scoped logger interface for use with ScopedValue.
 */
public interface ScopedVFL extends VFL {
    void run(String blockName, String blockMessage, Runnable runnable);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable, Executor executor);

    <R> R call(String blockName, String blockMessage, Callable<R> callable);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Callable<R> callable, Executor executor);
}
//TODO_OLD support for multi thread environment by explicitly passing context. DONE! we created async version of the methods and pass the conext to the thread where it will be running
//TODO throw exceptions for fn calls but handle it gracefully within the logger too
//TODO_OLD use synchronise? research and think properly before deciding this ANSWER = NO
//TODO fluent api to reduce verbosity and to get rid of overloaded functions