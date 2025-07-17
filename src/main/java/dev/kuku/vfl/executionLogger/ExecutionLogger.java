package dev.kuku.vfl.executionLogger;

import dev.kuku.vfl.core.BaseLogger;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ExecutionLogger extends BaseLogger {
    void run(String blockName, String message, Consumer<ExecutionLogger> runnable);

    void runHere(String blockName, String message, Consumer<ExecutionLogger> runnable);

    <R> R call(String blockName, String message, Function<R, String> endMessageFn, Function<ExecutionLogger, R> callable);

    <R> R call(String blockName, String message, Function<ExecutionLogger, R> callable);

    <R> R callHere(String blockName, String message, Function<R, String> endMessageFn, Function<ExecutionLogger, R> callable);

    <R> R callHere(String blockName, String message, Function<ExecutionLogger, R> callable);
}
