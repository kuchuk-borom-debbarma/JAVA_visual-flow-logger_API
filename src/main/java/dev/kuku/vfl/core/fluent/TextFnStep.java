package dev.kuku.vfl.core.fluent;

import dev.kuku.vfl.core.IVFL;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class TextFnStep<R> implements ITextFnStep<R> {
    private final Callable<R> callable;
    private final IVFL logger;
    private final Function<R, String> msgProcessorFn;

    public TextFnStep(Callable<R> callable, IVFL logger, Function<R, String> msgProcessorFn) {
        this.callable = callable;
        this.logger = logger;
        this.msgProcessorFn = msgProcessorFn;
    }

    @Override
    public R msg() {
        return this.logger.msgFn(callable, msgProcessorFn);
    }

    @Override
    public R warn() {
        return this.logger.warnFn(callable, msgProcessorFn);
    }

    @Override
    public R error() {
        return this.logger.errorFn(callable, msgProcessorFn);
    }
}
