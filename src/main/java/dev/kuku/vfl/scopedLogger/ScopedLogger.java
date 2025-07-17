package dev.kuku.vfl.scopedLogger;

import dev.kuku.vfl.core.BaseLogger;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Scoped logger interface for use with ScopedValue.
 */
public interface ScopedLogger extends BaseLogger {
    void run(String blockName, String message, Runnable runnable);

    void runHere(String blockName, String message, Runnable runnable);

    <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T call(String blockName, String message, Callable<T> callable);

    <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T callHere(String blockName, String message, Callable<T> callable);

}

