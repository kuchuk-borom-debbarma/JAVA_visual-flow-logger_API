package dev.kuku.vfl.core;

import java.util.concurrent.Callable;

class BlockLoggerImpl implements BlockLogger {
    @Override
    public <T> T call(Callable<T> callable) {
        return null;
    }

    @Override
    public <T> T callHere(Callable<T> callable) {
        return null;
    }

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
    public void run(Runnable runnable) {

    }

    @Override
    public void runHere(Runnable runnable) {

    }
}
