package dev.kuku.vfl.core.fluent_api;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public class FluentVFLCallable extends FluentVFL {
    private final VFLCallable vfl;

    public FluentVFLCallable(VFLCallable logger) {
        super(logger);
        this.vfl = logger;
    }

    @Override
    public <R> FnStepCallable<R> call(Supplier<R> fn) {
        return new FnStepCallable<>(fn);
    }

    public RunnableStep run(Runnable r) {
        return new RunnableStep(r);
    }

    public class FnStepCallable<R> extends FnStep<R> {
        public FnStepCallable(Supplier<R> fn) {
            super(fn);
        }

        public SubBlockStep asSubBlock(String blockName) {
            return new SubBlockStep(blockName);
        }

        public class SubBlockStep {
            private final String blockName;
            private String startMessage = null;
            private Function<R, String> endMessageSerializer = null;

            public SubBlockStep(String blockName) {
                this.blockName = blockName;
            }

            public SubBlockStep withStartMessage(String message) {
                this.startMessage = message;
                return this;
            }

            public SubBlockStep withEndMessageSerializer(Function<R, String> f) {
                this.endMessageSerializer = f;
                return null;
            }

            public R startPrimary() {
                return vfl.callPrimarySubBlock(blockName, startMessage, fn::get, endMessageSerializer);
            }

            public CompletableFuture<R> startJoiningSecondary(Executor executor) {
                return vfl.callSecondaryJoiningBlock(blockName, startMessage, fn::get, endMessageSerializer, executor);
            }

            public CompletableFuture<Void> startNonJoiningSecondary(Executor executor) {
                return vfl.callSecondaryNonJoiningBlock(blockName, startMessage, fn::get, executor);
            }
        }
    }

    public class RunnableStep {
        private final Runnable fn;

        public RunnableStep(Runnable fn) {
            this.fn = fn;
        }

        public BlockNameStep asSubBlock(String blockName) {
            return new BlockNameStep(blockName);
        }

        public class BlockNameStep {

            private final String blockName;
            private String startMessage = null;

            public BlockNameStep(String blockName) {
                this.blockName = blockName;
            }

            public BlockNameStep withStartMessage(String startMessage) {
                this.startMessage = startMessage;
                return this;
            }

            private Callable<Void> toCallable() {
                return () -> {
                    fn.run();
                    return null;
                };
            }

            public void startPrimary() {
                vfl.callPrimarySubBlock(blockName, startMessage, toCallable(), null);
            }

            public CompletableFuture<Void> startSecondaryJoining(Executor executor) {
                return vfl.callSecondaryJoiningBlock(blockName, startMessage, toCallable(), null, executor);
            }

            public CompletableFuture<Void> startNonJoiningSecondary(Executor executor) {
                return vfl.callSecondaryJoiningBlock(blockName, startMessage, toCallable(), null, executor);
            }
        }
    }
}
