package dev.kuku.vfl.scoped;

import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.VFLFluentAPI;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Fluent API for IScopedVFL providing block operations with scoped execution.
 * <p>
 * Usage examples:
 * - fluent.startBlock("myBlock").run(() -> {...})
 * - fluent.startBlock("myBlock").withMsg("Starting process").run(() -> {...})
 * - fluent.startBlock("myBlock").andCall(() -> computeValue()).call()
 * - fluent.startBlock("myBlock").withMsg("Processing").asAsync().run(() -> {...})
 * - fluent.startBlock("myBlock").andCall(() -> processData()).withEndMsg(r -> "Result: " + r).call()
 */

public class ScopedVFLFluent extends VFLFluentAPI {

    private static ScopedVFLFluent instance;

    private ScopedVFLFluent() {
        //Passing no logger. instead we will override getLogger() to give it scoped VFL instance
        super(null);
    }

    //Overriding get logger to pass current scope's logger. This function is used to get the logger before logging operations in parent class
    @Override
    protected IVFL getLogger() {
        return ScopedVFL.get();
    }

    public static ScopedVFLFluent get() {
        if (instance == null) {
            instance = new ScopedVFLFluent();
        }
        return instance;
    }

    /**
     * Start a scoped block operation
     */
    public ScopedBlockStep startBlock(String blockName) {
        return new ScopedBlockStep(blockName);
    }

    /**
     * Scoped block operation step
     */
    public static class ScopedBlockStep {
        private final String blockName;

        private ScopedBlockStep(String blockName) {
            this.blockName = blockName;
        }

        /**
         * Add optional message to the block
         */
        public ScopedBlockWithMsgStep withMsg(String message) {
            return new ScopedBlockWithMsgStep(blockName, message);
        }

        /**
         * Run scoped block operation without message
         */
        public void run(Runnable runnable) {
            ScopedVFL.get().run(blockName, null, runnable);
        }

        /**
         * Start a scoped call operation without message
         */
        public <R> ScopedCallStep<R> andCall(Callable<R> callable) {
            return new ScopedCallStep<>(blockName, null, callable);
        }

        /**
         * Make this scoped block operation async
         */
        public AsyncScopedBlockStep asAsync() {
            return new AsyncScopedBlockStep(blockName);
        }
    }

    /**
     * Scoped block step with message
     */
    public static class ScopedBlockWithMsgStep {
        private final String blockName;
        private final String message;

        private ScopedBlockWithMsgStep(String blockName, String message) {
            this.blockName = blockName;
            this.message = message;
        }

        /**
         * Run scoped block operation with message
         */
        public void run(Runnable runnable) {
            ScopedVFL.get().run(blockName, message, runnable);
        }

        /**
         * Start a scoped call operation with message
         */
        public <R> ScopedCallStep<R> andCall(Callable<R> callable) {
            return new ScopedCallStep<>(blockName, message, callable);
        }

        /**
         * Make this scoped block operation async
         */
        public AsyncScopedBlockWithMsgStep asAsync() {
            return new AsyncScopedBlockWithMsgStep(blockName, message);
        }
    }

    /**
     * Scoped call operation step
     */
    public static class ScopedCallStep<R> {
        private final String blockName;
        private final String message;
        private final Callable<R> callable;

        private ScopedCallStep(String blockName, String message, Callable<R> callable) {
            this.blockName = blockName;
            this.message = message;
            this.callable = callable;
        }

        /**
         * Execute scoped call without end message transformation
         */
        public R call() {
            return ScopedVFL.get().call(blockName, message, Object::toString, callable);
        }

        /**
         * Add end message transformation
         */
        public ScopedCallWithEndMsgStep<R> withEndMsg(Function<R, String> endMessageFn) {
            return new ScopedCallWithEndMsgStep<>(blockName, message, endMessageFn, callable);
        }
    }

    /**
     * Scoped call step with end message transformation
     */
    public static class ScopedCallWithEndMsgStep<R> {
        private final String blockName;
        private final String message;
        private final Function<R, String> endMessageFn;
        private final Callable<R> callable;

        private ScopedCallWithEndMsgStep(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable) {
            this.blockName = blockName;
            this.message = message;
            this.endMessageFn = endMessageFn;
            this.callable = callable;
        }

        /**
         * Execute scoped call with end message transformation
         */
        public R call() {
            return ScopedVFL.get().call(blockName, message, endMessageFn, callable);
        }
    }

    /**
     * Async scoped block step without message
     */
    public static class AsyncScopedBlockStep {
        private final String blockName;
        private Executor executor;

        private AsyncScopedBlockStep(String blockName) {
            this.blockName = blockName;
        }

        /**
         * Set custom executor (optional)
         */
        public AsyncScopedBlockStep withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Run async scoped block operation
         */
        public CompletableFuture<Void> run(Runnable runnable) {
            return ScopedVFL.get().runAsync(blockName, null, runnable, executor);
        }

        /**
         * Start async scoped call operation
         */
        public <R> AsyncScopedCallStep<R> andCall(Callable<R> callable) {
            return new AsyncScopedCallStep<>(blockName, null, callable, executor);
        }
    }

    /**
     * Async scoped block step with message
     */
    public static class AsyncScopedBlockWithMsgStep {
        private final String blockName;
        private final String message;
        private Executor executor;

        private AsyncScopedBlockWithMsgStep(String blockName, String message) {
            this.blockName = blockName;
            this.message = message;
        }

        /**
         * Set custom executor (optional)
         */
        public AsyncScopedBlockWithMsgStep withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Run async scoped block operation with message
         */
        public CompletableFuture<Void> run(Runnable runnable) {
            return ScopedVFL.get().runAsync(blockName, message, runnable, executor);
        }

        /**
         * Start async scoped call operation with message
         */
        public <R> AsyncScopedCallStep<R> andCall(Callable<R> callable) {
            return new AsyncScopedCallStep<>(blockName, message, callable, executor);
        }
    }

    /**
     * Async scoped call operation step
     */
    public static class AsyncScopedCallStep<R> {
        private final String blockName;
        private final String message;
        private final Callable<R> callable;
        private final Executor executor;

        private AsyncScopedCallStep(String blockName, String message, Callable<R> callable, Executor executor) {
            this.blockName = blockName;
            this.message = message;
            this.callable = callable;
            this.executor = executor;
        }

        /**
         * Execute async scoped call without end message transformation
         */
        public CompletableFuture<R> call() {
            return ScopedVFL.get().callAsync(blockName, message, Object::toString, callable, executor);
        }

        /**
         * Add end message transformation for async scoped call
         */
        public AsyncScopedCallWithEndMsgStep<R> withEndMsg(Function<R, String> endMessageFn) {
            return new AsyncScopedCallWithEndMsgStep<>(blockName, message, endMessageFn, callable, executor);
        }
    }

    /**
     * Async scoped call step with end message transformation
     */
    public static class AsyncScopedCallWithEndMsgStep<R> {
        private final String blockName;
        private final String message;
        private final Function<R, String> endMessageFn;
        private final Callable<R> callable;
        private final Executor executor;

        private AsyncScopedCallWithEndMsgStep(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable, Executor executor) {
            this.blockName = blockName;
            this.message = message;
            this.endMessageFn = endMessageFn;
            this.callable = callable;
            this.executor = executor;
        }

        /**
         * Execute async scoped call with end message transformation
         */
        public CompletableFuture<R> call() {
            return ScopedVFL.get().callAsync(blockName, message, endMessageFn, callable, executor);
        }
    }
}