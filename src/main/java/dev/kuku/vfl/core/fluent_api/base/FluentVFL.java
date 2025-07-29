package dev.kuku.vfl.core.fluent_api.base;

import dev.kuku.vfl.core.fluent_api.base.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.function.Supplier;

public class FluentVFL {
    protected final VFL vfl;

    public FluentVFL(VFL logger) {
        vfl = logger;
    }

    public void log(String message) {
        vfl.log(message);
    }

    public void warn(String message) {
        vfl.warn(message);
    }

    public void error(String message) {
        vfl.error(message);
    }

    public <R> SupplierStep<R> call(Supplier<R> fn) {
        return new SupplierStep<>(vfl, fn);
    }
}
