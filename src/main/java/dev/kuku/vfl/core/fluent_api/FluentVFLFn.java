package dev.kuku.vfl.core.fluent_api;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Consumer;
import java.util.function.Function;

public class FluentVFLFn extends FluentVFL {
    private final VFLFn vfl;

    public FluentVFLFn(VFLFn vfl) {
        super(vfl);
        this.vfl = vfl;
    }

    public StartBlockStep startSubBlock(String blockName) {
        return new StartBlockStep(blockName);
    }

    public class StartBlockStep {
        private final String blockName;
        private String message;

        public StartBlockStep(String blockName) {
            this.blockName = blockName;
        }

        public StartBlockStep withStartMessage(String message) {
            this.message = message;
            return this;
        }

        public <R> FnStep<R> forFunction(Function<VFLFn, R> fn) {
            return new FnStep<>(blockName, message, fn);
        }

        public FnStepVoid forConsumer(Consumer<VFLFn> fn) {
            return new FnStepVoid(blockName, message, fn);
        }
    }

    public class FnStep<R> {
        private final String blockName;
        private final String message;
        private final Function<VFLFn, R> fn;
        private Function<R, String> endMsg;

        private FnStep(String blockName, String message, Function<VFLFn, R> fn) {
            this.blockName = blockName;
            this.message = message;
            this.fn = fn;
        }

        public FnStep<R> withEndMessage(Function<R, String> endMessageSerializer) {
            this.endMsg = endMessageSerializer;
            return this;
        }

        public R executeAsPrimary() {
            return vfl.callPrimarySubBlock(blockName, message, fn, endMsg);
        }

        public R executeAsJoiningSecondary() {
            return vfl.callSecondaryJoiningBlock(blockName, message, fn, endMsg);
        }

        public R executeAsNonJoiningSecondary() {
            return vfl.callSecondaryNonJoiningBlock(blockName, message, fn, endMsg);
        }
    }

    public class FnStepVoid {
        private final String blockName;
        private final String message;
        private final Consumer<VFLFn> fn;

        private FnStepVoid(String blockName, String message, Consumer<VFLFn> fn) {
            this.blockName = blockName;
            this.message = message;
            this.fn = fn;
        }

        private Function<VFLFn, Void> toVoidFunction() {
            return vflFn -> {
                fn.accept(vflFn);
                return null;
            };
        }

        public void executeAsPrimary() {
            vfl.callPrimarySubBlock(blockName, message, toVoidFunction(), null);
        }

        public void executeAsJoiningSecondary() {
            vfl.callSecondaryJoiningBlock(blockName, message, toVoidFunction(), null);
        }

        public void executeAsNonJoiningSecondary() {
            vfl.callSecondaryNonJoiningBlock(blockName, message, toVoidFunction(), null);
        }
    }
}