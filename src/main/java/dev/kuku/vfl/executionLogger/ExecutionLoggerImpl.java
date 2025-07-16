package dev.kuku.vfl.executionLogger;

import dev.kuku.vfl.core.BlockLog;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class ExecutionLoggerImpl implements BlockLog {

    @Override
    public void text(String message) {

    }

    @Override
    public void textHere(String message) {

    }

    @Override
    public void warn(String message) {

    }

    @Override
    public void warnHere(String message) {

    }

    @Override
    public void error(String message) {

    }

    @Override
    public void errorHere(String message) {

    }

    @Override
    public void run(String blockName, String message, Runnable runnable) {

    }

    @Override
    public void runHere(String blockName, String message, Runnable runnable) {

    }

    @Override
    public <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return null;
    }

    @Override
    public <T> T call(String blockName, String message, Callable<T> callable) {
        return null;
    }

    @Override
    public <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return null;
    }

    @Override
    public <T> T callHere(String blockName, String message, Callable<T> callable) {
        return null;
    }

    @Override
    public void closeBlock(String endMessage) {

    }
}
