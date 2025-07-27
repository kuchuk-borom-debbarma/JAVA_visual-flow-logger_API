package dev.kuku.vfl.core.fluent_api;

import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.function.Function;
import java.util.function.Supplier;

public class FluentVFL {
    private final VFL vfl;

    public FluentVFL(VFL logger) {
        vfl = logger;
    }

    public LogTextStep logText(String text) {
        return new LogTextStep(text);
    }

    public <R> FnStep logFn(Supplier<R> fn) {
        return new FnStep<>(fn);
    }

    public class LogTextStep {
        private final String text;

        public LogTextStep(String text) {
            this.text = text;
        }

        public void asMessage() {
            vfl.log(text);
        }

        public void asError() {
            vfl.error(text);
        }

        public void asWarning() {
            vfl.warn(text);
        }
    }

    public class FnStep<R> {
        private final Supplier<R> fn;

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
