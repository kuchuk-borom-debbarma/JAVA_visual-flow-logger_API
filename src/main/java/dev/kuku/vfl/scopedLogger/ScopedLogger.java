package dev.kuku.vfl.scopedLogger;

import dev.kuku.vfl.core.BaseLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Scoped logger interface for use with ScopedValue.
 */
public interface ScopedLogger extends BaseLogger {
    void run(String blockName, String message, Runnable runnable);

    Future<Void> runAsync(String blockName, String message, Runnable runnable);

    Future<Void> runAsync(String blockName, String message, Runnable runnable, Executor executor);

    void runHere(String blockName, String message, Runnable runnable);

    Future<Void> runHereAsync(String blockName, String message, Runnable runnable);

    Future<Void> runHereAsync(String blockName, String message, Runnable runnable, Executor executor);

    <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> Future<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> Future<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable, Executor executor);

    <T> T call(String blockName, String message, Callable<T> callable);

    <T> Future<T> callAsync(String blockName, String message, Callable<T> callable);

    <T> Future<T> callAsync(String blockName, String message, Callable<T> callable, Executor executor);

    <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> Future<T> callHereAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> Future<T> callHereAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable, Executor executor);

    <T> T callHere(String blockName, String message, Callable<T> callable);

    <T> Future<T> callHereAsync(String blockName, String message, Callable<T> callable);

    <T> Future<T> callHereAsync(String blockName, String message, Callable<T> callable, Executor executor);
}
//TODO support for multi thread environment by explicitly passing context
//TODO throw exceptions for fn calls but handle it gracefully within the logger too
//TODO use synchoroinse? research and think properly before deciding this