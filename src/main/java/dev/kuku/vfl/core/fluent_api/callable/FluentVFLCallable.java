package dev.kuku.vfl.core.fluent_api.callable;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.fluent_api.callable.steps.CallableSupplierStep;
import dev.kuku.vfl.core.fluent_api.callable.steps.runnable.RunnableForVFLCallableStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.function.Supplier;

public class FluentVFLCallable extends FluentVFL {
    private final VFLCallable vfl;

    public FluentVFLCallable(VFLCallable logger) {
        super(logger);
        this.vfl = logger;
    }

    @Override
    public <R> CallableSupplierStep<R> call(Supplier<R> fn) {
        return new CallableSupplierStep<>(vfl, fn);
    }

    @Override
    public RunnableForVFLCallableStep run(Runnable r) {
        return new RunnableForVFLCallableStep(r, vfl);
    }
}
