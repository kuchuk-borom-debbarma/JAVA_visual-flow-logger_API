package dev.kuku.vfl.core.fluent;

import dev.kuku.vfl.core.IVFL;

import java.util.concurrent.Callable;

public class VFLFluentAPI {
    private final IVFL logger;

    public VFLFluentAPI(IVFL logger) {
        this.logger = logger;
    }

    public <R> IFnTextStep<R> fn(Callable<R> callable) {
        return new FnTextStep<>(callable, this.logger);
    }

    public void msg(String msg) {
        logger.msg(msg);
    }

    public void error(String error) {
        logger.error(error);
    }

    public void warn(String warn) {
        logger.warn(warn);
    }
}