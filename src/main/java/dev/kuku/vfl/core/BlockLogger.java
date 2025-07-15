package dev.kuku.vfl.core;

import java.util.concurrent.Callable;

public interface BlockLogger {
    <T> T call(Callable<T> callable);

    <T> T callHere(Callable<T> callable);

    void text(String message);

    void textHere(String message);

    void warn(String message);

    void warnHere(String message);

    void error(String message);

    void errorHere(String message);

    void run(Runnable runnable);

    void runHere(Runnable runnable);
}
