package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.impl.threadlocal.flient_steps.ThreadVFLRunnableAsyncStep;
import dev.kuku.vfl.impl.threadlocal.flient_steps.ThreadVFLRunnableStep;
import dev.kuku.vfl.impl.threadlocal.flient_steps.ThreadVFLSupplierAsyncStep;
import dev.kuku.vfl.impl.threadlocal.flient_steps.ThreadVFLSupplierStep;

import java.util.function.Supplier;

public class FluentThreadVFL extends FluentVFL {
    protected FluentThreadVFL() {
        super(ThreadVFL.getCurrentLogger());
    }

    @Override
    public <R> ThreadVFLSupplierStep<R> call(Supplier<R> fn) {
        return new ThreadVFLSupplierStep<>(ThreadVFL.getCurrentLogger(), fn);
    }

    public <R> ThreadVFLSupplierAsyncStep<R> callAsync(Supplier<R> fn) {
        return new ThreadVFLSupplierAsyncStep<>(ThreadVFL.getCurrentLogger(), fn);
    }

    @Override
    public <R> ThreadVFLRunnableStep run(Runnable runnable) {
        return new ThreadVFLRunnableStep(runnable, vfl);
    }

    public ThreadVFLRunnableAsyncStep runAsync(Runnable runnable) {
        return new ThreadVFLRunnableAsyncStep(vfl, runnable);
    }

}
