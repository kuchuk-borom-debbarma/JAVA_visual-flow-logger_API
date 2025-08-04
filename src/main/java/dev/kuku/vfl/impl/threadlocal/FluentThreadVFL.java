package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.impl.threadlocal.flient_steps.CallableRunnableStep;
import dev.kuku.vfl.impl.threadlocal.flient_steps.CallableRunnableStepAsync;
import dev.kuku.vfl.impl.threadlocal.flient_steps.CallableSupplierStep;
import dev.kuku.vfl.impl.threadlocal.flient_steps.CallableSupplierStepAsync;

import java.util.function.Supplier;

public class FluentThreadVFL extends FluentVFL {
    protected FluentThreadVFL() {
        super(ThreadVFL.getCurrentLogger());
    }

    @Override
    public <R> CallableSupplierStep<R> call(Supplier<R> fn) {
        return new CallableSupplierStep<>(ThreadVFL.getCurrentLogger(), fn);
    }

    public <R> CallableSupplierStepAsync<R> callAsync(Supplier<R> fn) {
        return new CallableSupplierStepAsync<R>(ThreadVFL.getCurrentLogger(), fn);
    }

    @Override
    public <R> CallableRunnableStep run(Runnable runnable) {
        return new CallableRunnableStep(runnable, vfl);
    }

    public CallableRunnableStepAsync runAsync(Runnable runnable) {
        return new CallableRunnableStepAsync(runnable, vfl);
    }

}
