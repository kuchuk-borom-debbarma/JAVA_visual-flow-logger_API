package dev.kuku.vfl.core.fluent_api.base;

import dev.kuku.vfl.core.fluent_api.base.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.function.Function;
import java.util.function.Supplier;

public class FluentVFL {
    private final VFL vfl;

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

    public class FnStep<R> {
        protected final Supplier<R> fn;

        public FnStep(Supplier<R> fn) {
            this.fn = fn;
        }

        public R asMessage(Function<R, String> messageSerializer) {
            return vfl.logFn(fn, messageSerializer);

        }

        public R asError(Function<R, String> errorMessageSerializer) {
            return vfl.errorFn(fn, errorMessageSerializer);
        }

        public R asWarning(Function<R, String> warningMessageSerializer) {
            return vfl.warnFn(fn, warningMessageSerializer);
        }
    }
}
