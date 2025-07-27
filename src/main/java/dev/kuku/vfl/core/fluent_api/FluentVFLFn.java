package dev.kuku.vfl.core.fluent_api;

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
        private String message = null;

        public StartBlockStep(String blockName) {
            this.blockName = blockName;
        }

        public StartBlockStep withStartMessage(String message) {
            this.message = message;
            return this;
        }

        public <R> FnStep<R> forFunction(Function<VFLFn, R> fn) {
            return new FnStep<>(fn);
        }

        public FnStepVoid forFunction(Consumer<VFLFn> fn) {
            return new FnStepVoid(fn);
        }

        public class FnStep<R> {
            private final Function<VFLFn, R> fn;
            private Function<R, String> endMsg = null;

            public FnStep(Function<VFLFn, R> fn) {
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
            private final Consumer<VFLFn> fn;

            public FnStepVoid(Consumer<VFLFn> fn) {
                this.fn = fn;
            }

            public void executeAsPrimary() {
                vfl.callPrimarySubBlock(blockName, message, o -> {
                    fn.accept(o);
                    return null;
                }, null);
            }

            public void executeAsJoiningSecondary() {
                vfl.callSecondaryJoiningBlock(blockName, message, o -> {
                    fn.accept(o);
                    return null;
                }, null);
            }

            public void executeAsNonJoiningSecondary() {
                vfl.callSecondaryNonJoiningBlock(blockName, message, o -> {
                    fn.accept(o);
                    return null;
                }, null);
            }
        }
    }
}
