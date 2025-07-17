package dev.kuku.vfl.multiThreadedScopedLogger;

import dev.kuku.vfl.core.BaseLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Scoped logger interface for use with ScopedValue.
 */
public interface ScopedLogger extends BaseLogger {
    void run(String blockName, String message, Runnable runnable);

//    Future<Void> runAsync(String blockName, String message, Runnable runnable);

    void runHere(String blockName, String message, Runnable runnable);

    <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T call(String blockName, String message, Callable<T> callable);

    <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T callHere(String blockName, String message, Callable<T> callable);
}
//TODO support for multi thread environment by explicitly passing context
//TODO thread safety
