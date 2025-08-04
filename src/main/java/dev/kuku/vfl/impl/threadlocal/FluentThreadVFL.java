package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;

import java.util.function.Supplier;

public class FluentThreadVFL extends FluentVFL {
    protected FluentThreadVFL() {
        super(ThreadVFL.getCurrentLogger());
    }

    @Override
    public <R> CallableSupplierStep<R> call(Supplier<R> fn) {
        return new CallableSupplierStep<>(ThreadVFL.getCurrentLogger(), fn);
    }

    @Override
    public <R> CallableRunnableStep run(Runnable runnable) {
        return new CallableRunnableStep(runnable, vfl);
    }
}
