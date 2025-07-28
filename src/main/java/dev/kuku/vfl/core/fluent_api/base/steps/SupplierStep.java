package dev.kuku.vfl.core.fluent_api.base.steps;

import dev.kuku.vfl.core.vfl_abstracts.VFL;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class SupplierStep<R> {
    protected final VFL vfl;
    protected final Supplier<R> supplier;

    public R asLog(Function<R, String> messageSerializer) {
        return vfl.logFn(supplier, messageSerializer);

    }

    public R asError(Function<R, String> errorMessageSerializer) {
        return vfl.errorFn(supplier, errorMessageSerializer);
    }

    public R asWarning(Function<R, String> warningMessageSerializer) {
        return vfl.warnFn(supplier, warningMessageSerializer);
    }
}
