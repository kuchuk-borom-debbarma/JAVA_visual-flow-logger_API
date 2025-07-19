package dev.kuku.vfl.core.fluent;

import dev.kuku.vfl.core.IVFL;

import java.util.concurrent.Callable;

public class TextStep implements ITextStep {
    private final IVFL logger;

    public TextStep(IVFL logger) {
        this.logger = logger;
    }

    @Override
    public <R> IFnTextStep<R> fn(Callable<R> callable) {
        return new FnTextStep<>(callable, this.logger);
    }

    @Override
    public void msg(String msg) {
        logger.msg(msg);
    }

    @Override
    public void error(String error) {
        logger.error(error);
    }

    @Override
    public void warn(String warn) {
        logger.warn(warn);
    }
}