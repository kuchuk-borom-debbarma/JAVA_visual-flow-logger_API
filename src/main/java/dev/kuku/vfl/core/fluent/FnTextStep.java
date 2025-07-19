package dev.kuku.vfl.core.fluent;

import dev.kuku.vfl.core.IVFL;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class FnTextStep<R> implements IFnTextStep<R> {
    private final Callable<R> callable;
    private final IVFL logger;

    public FnTextStep(Callable<R> callable, IVFL logger) {
        this.callable = callable;
        this.logger = logger;
    }

    @Override
    public ITextFnStep<R> textFn(Function<R, String> fn) {
        return new TextFnStep<R>(callable, logger, fn);
    }
}