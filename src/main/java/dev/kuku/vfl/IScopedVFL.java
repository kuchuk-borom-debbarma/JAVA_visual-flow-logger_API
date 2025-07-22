package dev.kuku.vfl;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public interface IScopedVFL extends IVFL {
    void run(String blockName, String blockMessage, Runnable runnable);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable, Executor executor);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable);

    <R> R call(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable, Executor executor);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable);
}