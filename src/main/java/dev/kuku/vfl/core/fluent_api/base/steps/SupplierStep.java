package dev.kuku.vfl.core.fluent_api.base.steps;

import dev.kuku.vfl.core.vfl_abstracts.VFL;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class SupplierStep<R> {
    protected final VFL vfl;
    protected final Supplier<R> supplier;

    public R asLog(Function<R, String> messageSerializer, Object... args) {
        return vfl.logFn(supplier, messageSerializer, args);
    }

    public R asError(Function<R, String> errorMessageSerializer, Object... args) {
        return vfl.errorFn(supplier, errorMessageSerializer, args);
    }

    public R asWarning(Function<R, String> warningMessageSerializer, Object... args) {
        return vfl.warnFn(supplier, warningMessageSerializer, args);
    }
}
