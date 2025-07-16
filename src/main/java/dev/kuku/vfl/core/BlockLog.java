package dev.kuku.vfl.core;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Simple logger for logging strings.
 */
public interface BlockLog {
    void text(String message);

    void textHere(String message);

    void warn(String message);

    void warnHere(String message);

    void error(String message);

    void errorHere(String message);

    void run(String blockName, String message, Runnable runnable);

    void runHere(String blockName, String message, Runnable runnable);

    <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T call(String blockName, String message, Callable<T> callable);

    <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable);

    <T> T callHere(String blockName, String message, Callable<T> callable);

    void closeBlock(String endMessage);
}
