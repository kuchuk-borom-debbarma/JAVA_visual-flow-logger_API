package dev.kuku.vfl.fluent;

import dev.kuku.vfl.IPassthroughVFL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Extended fluent API for IPassthroughVFL providing block operations with async support.
 * <p>
 * Usage examples:
 * - fluent.startBlock("myBlock").run(logger -> {...})
 * - fluent.startBlock("myBlock").withMsg("Starting process").run(logger -> {...})
 * - fluent.startBlock("myBlock").andCall(logger -> processData()).call()
 * - fluent.startBlock("myBlock").withMsg("Processing").asAsync().run(logger -> {...})
 * - fluent.startBlock("myBlock").andCall(logger -> compute()).withEndMsg(r -> "Result: " + r).call()
 */
public class PassthroughFluentAPI extends VFLFluentAPI {
    private final IPassthroughVFL passthroughLogger;

    public PassthroughFluentAPI(IPassthroughVFL logger) {
        super(logger);
        this.passthroughLogger = logger;
    }

    /**
     * Start a block operation
     */
    public BlockStep startBlock(String blockName) {
        return new BlockStep(blockName);
    }

    /**
     * Block operation step
     */
    public class BlockStep {
        private final String blockName;

        private BlockStep(String blockName) {
            this.blockName = blockName;
        }

        /**
         * Add optional message to the block
         */
        public BlockWithMsgStep withMsg(String message) {
            return new BlockWithMsgStep(blockName, message);
        }

        /**
         * Run block operation without message
         */
        public void run(Consumer<IPassthroughVFL> fn) {
            passthroughLogger.run(blockName, null, fn);
        }

        /**
         * Start a call operation without message
         */
        public <R> CallStep<R> andCall(Function<IPassthroughVFL, R> fn) {
            return new CallStep<>(blockName, null, fn);
        }

        /**
         * Make this block operation async
         */
        public AsyncBlockStep asAsync() {
            return new AsyncBlockStep(blockName);
        }
    }

    /**
     * Block step with message
     */
    public class BlockWithMsgStep {
        private final String blockName;
        private final String message;

        private BlockWithMsgStep(String blockName, String message) {
            this.blockName = blockName;
            this.message = message;
        }

        /**
         * Run block operation with message
         */
        public void run(Consumer<IPassthroughVFL> fn) {
            passthroughLogger.run(blockName, message, fn);
        }

        /**
         * Start a call operation with message
         */
        public <R> CallStep<R> andCall(Function<IPassthroughVFL, R> fn) {
            return new CallStep<>(blockName, message, fn);
        }

        /**
         * Make this block operation async
         */
        public AsyncBlockWithMsgStep asAsync() {
            return new AsyncBlockWithMsgStep(blockName, message);
        }
    }

    /**
     * Call operation step
     */
    public class CallStep<R> {
        private final String blockName;
        private final String message;
        private final Function<IPassthroughVFL, R> fn;

        private CallStep(String blockName, String message, Function<IPassthroughVFL, R> fn) {
            this.blockName = blockName;
            this.message = message;
            this.fn = fn;
        }

        /**
         * Execute call without end message transformation
         */
        public R call() {
            return passthroughLogger.call(blockName, message, Object::toString, fn);
        }

        /**
         * Add end message transformation
         */
        public CallWithEndMsgStep<R> withEndMsg(Function<R, String> endMessageFn) {
            return new CallWithEndMsgStep<>(blockName, message, endMessageFn, fn);
        }
    }

    /**
     * Call step with end message transformation
     */
    public class CallWithEndMsgStep<R> {
        private final String blockName;
        private final String message;
        private final Function<R, String> endMessageFn;
        private final Function<IPassthroughVFL, R> fn;

        private CallWithEndMsgStep(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn) {
            this.blockName = blockName;
            this.message = message;
            this.endMessageFn = endMessageFn;
            this.fn = fn;
        }

        /**
         * Execute call with end message transformation
         */
        public R call() {
            return passthroughLogger.call(blockName, message, endMessageFn, fn);
        }
    }

    /**
     * Async block step without message
     */
    public class AsyncBlockStep {
        private final String blockName;
        private Executor executor;

        private AsyncBlockStep(String blockName) {
            this.blockName = blockName;
        }

        /**
         * Set custom executor (optional)
         */
        public AsyncBlockStep withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Run async block operation
         */
        public CompletableFuture<Void> run(Consumer<IPassthroughVFL> fn) {
            return passthroughLogger.runAsync(blockName, null, fn, executor);
        }

        /**
         * Start async call operation
         */
        public <R> AsyncCallStep<R> andCall(Function<IPassthroughVFL, R> fn) {
            return new AsyncCallStep<>(blockName, null, fn, executor);
        }
    }

    /**
     * Async block step with message
     */
    public class AsyncBlockWithMsgStep {
        private final String blockName;
        private final String message;
        private Executor executor;

        private AsyncBlockWithMsgStep(String blockName, String message) {
            this.blockName = blockName;
            this.message = message;
        }

        /**
         * Set custom executor (optional)
         */
        public AsyncBlockWithMsgStep withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Run async block operation with message
         */
        public CompletableFuture<Void> run(Consumer<IPassthroughVFL> fn) {
            return passthroughLogger.runAsync(blockName, message, fn, executor);
        }

        /**
         * Start async call operation with message
         */
        public <R> AsyncCallStep<R> andCall(Function<IPassthroughVFL, R> fn) {
            return new AsyncCallStep<>(blockName, message, fn, executor);
        }
    }

    /**
     * Async call operation step
     */
    public class AsyncCallStep<R> {
        private final String blockName;
        private final String message;
        private final Function<IPassthroughVFL, R> fn;
        private final Executor executor;

        private AsyncCallStep(String blockName, String message, Function<IPassthroughVFL, R> fn, Executor executor) {
            this.blockName = blockName;
            this.message = message;
            this.fn = fn;
            this.executor = executor;
        }

        /**
         * Execute async call without end message transformation
         */
        public CompletableFuture<R> call() {
            return passthroughLogger.callAsync(blockName, message, Object::toString, fn, executor);
        }

        /**
         * Add end message transformation for async call
         */
        public AsyncCallWithEndMsgStep<R> withEndMsg(Function<R, String> endMessageFn) {
            return new AsyncCallWithEndMsgStep<>(blockName, message, endMessageFn, fn, executor);
        }
    }

    /**
     * Async call step with end message transformation
     */
    public class AsyncCallWithEndMsgStep<R> {
        private final String blockName;
        private final String message;
        private final Function<R, String> endMessageFn;
        private final Function<IPassthroughVFL, R> fn;
        private final Executor executor;

        private AsyncCallWithEndMsgStep(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn, Executor executor) {
            this.blockName = blockName;
            this.message = message;
            this.endMessageFn = endMessageFn;
            this.fn = fn;
            this.executor = executor;
        }

        /**
         * Execute async call with end message transformation
         */
        public CompletableFuture<R> call() {
            return passthroughLogger.callAsync(blockName, message, endMessageFn, fn, executor);
        }
    }
}