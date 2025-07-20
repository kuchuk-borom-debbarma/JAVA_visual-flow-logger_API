package dev.kuku.vfl.scoped;

import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.VFLFluentAPI;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Static fluent API for IScopedVFL providing block operations with scoped execution.
 * Automatically uses the current scope's logger instance via ScopedVFL.get().
 * <p>
 * Usage examples:
 * - ScopedFluentAPI.startBlock("myBlock").run(() -> {...})
 * - ScopedFluentAPI.startBlock("myBlock").withMsg("Starting process").run(() -> {...})
 * - ScopedFluentAPI.startBlock("myBlock").andCall(() -> computeValue()).call()
 * - ScopedFluentAPI.startBlock("myBlock").withMsg("Processing").asAsync().run(() -> {...})
 * - ScopedFluentAPI.startBlock("myBlock").andCall(() -> processData()).withEndMsg(r -> "Result: " + r).call()
 */
public final class ScopedFluentAPI extends VFLFluentAPI {

    @Override
    protected IVFL getLogger() {
        return ScopedVFL.scopedInstance.get();
    }

     // Private constructor - use static factory method
    private ScopedFluentAPI() {
        super(null); // We'll override getLogger() anyway
    }

    /**
     * Start a scoped block operation
     */
    public static ScopedBlockStep startBlock(String blockName) {
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
            getScopedLogger().run(blockName, null, runnable);
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
            getScopedLogger().run(blockName, message, runnable);
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
            return getScopedLogger().call(blockName, message, Object::toString, callable);
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
            return getScopedLogger().call(blockName, message, endMessageFn, callable);
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
            return getScopedLogger().runAsync(blockName, null, runnable, executor);
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
            return getScopedLogger().runAsync(blockName, message, runnable, executor);
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
            return getScopedLogger().callAsync(blockName, message, Object::toString, callable, executor);
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
            return getScopedLogger().callAsync(blockName, message, endMessageFn, callable, executor);
        }
    }

    /**
     * Helper method to get the current scoped logger instance.
     * This ensures we always use the current scope's logger, even if the scope changes.
     */
    private static IScopedVFL getScopedLogger() {
        return ScopedVFL.get();
    }
}