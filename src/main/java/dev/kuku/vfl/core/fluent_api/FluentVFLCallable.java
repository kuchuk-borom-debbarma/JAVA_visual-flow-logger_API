package dev.kuku.vfl.core.fluent_api;

import dev.kuku.vfl.core.models.VFLExecutionException;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class FluentVFLCallable extends FluentVFL {
    private final VFLCallable vfl;

    public FluentVFLCallable(VFLCallable logger) {
        super(logger);
        this.vfl = logger;
    }

    public StartBlockStep startSubBlock(String blockName) {
        return new StartBlockStep(blockName);
    }

    public class StartBlockStep {
        private final String blockName;
        private String startMessage = null;

        public StartBlockStep(String blockName) {
            this.blockName = blockName;
        }

        public StartBlockStep withStartMessage(String startMessage) {
            this.startMessage = startMessage;
            return this;
        }

        public <R> FnStep<R> forCallable(Callable<R> callable) {
            return new FnStep<>(callable);
        }

        public FnVoidStep forRunnable(Runnable runnable) {
            return new FnVoidStep(runnable);
        }

        public class FnStep<R> {
            private final Callable<R> callable;
            private Function<R, String> endMessageSerializer = null;

            public FnStep(Callable<R> callable) {
                this.callable = callable;
            }

            public FnStep<R> withEndMessageSerializer(Function<R, String> endMessageSerializer) {
                this.endMessageSerializer = endMessageSerializer;
                return this;
            }

            public R executeAsPrimary() {
                return vfl.callPrimarySubBlock(blockName, startMessage, callable, endMessageSerializer);
            }

            public CompletableFuture<R> executeAsJoiningSecondary(Executor executor) {
                return vfl.callSecondaryJoiningBlock(blockName, startMessage, callable, endMessageSerializer, executor);
            }

            public CompletableFuture<Void> executeAsNonJoiningSecondary(Executor executor) {
                return vfl.callSecondaryNonJoiningBlock(blockName, startMessage, () -> {
                    try {
                        callable.call();
                    } catch (Exception e) {
                        throw new VFLExecutionException(e);
                    }
                }, executor);
            }
        }

        public class FnVoidStep {
            private final Runnable runnable;

            public FnVoidStep(Runnable runnable) {
                this.runnable = runnable;
            }

            private Callable<Void> toCallable() {
                return () -> {
                    runnable.run();
                    return null;
                };
            }

            public void executeAsPrimary() {
                vfl.callPrimarySubBlock(blockName, startMessage, toCallable(), null);
            }

            public CompletableFuture<Void> executeAsJoiningSecondary(Executor executor) {
                return vfl.callSecondaryJoiningBlock(blockName, startMessage, toCallable(), null, executor);
            }

            public CompletableFuture<Void> executeAsNonJoiningSecondary(Executor executor) {
                return vfl.callSecondaryNonJoiningBlock(blockName, startMessage, runnable, executor);
            }
        }
    }
}
