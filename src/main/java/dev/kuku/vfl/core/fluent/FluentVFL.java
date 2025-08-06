package dev.kuku.vfl.core.fluent;

import dev.kuku.vfl.core.fluent.steps.RunnableStep;
import dev.kuku.vfl.core.fluent.steps.SupplierStep;
import dev.kuku.vfl.core.VFL;

import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

public class FluentVFL {
    protected final VFL vfl;
    public FluentVFL(VFL logger) {
        vfl = logger;
    }

    public void log(String message, Object... args) {
        String finalMsg = FormatMessage(message, args);
        vfl.info(finalMsg);
    }

    public void warn(String message, Object... args) {
        vfl.warn(FormatMessage(message, args));
    }

    public void error(String message, Object... args) {
        vfl.error(FormatMessage(message, args));
    }

    public <R> SupplierStep<R> call(Supplier<R> fn) {
        return new SupplierStep<>(vfl, fn);
    }

    public <R> RunnableStep run(Runnable runnable) {
        return new RunnableStep(runnable, vfl);
    }
}
