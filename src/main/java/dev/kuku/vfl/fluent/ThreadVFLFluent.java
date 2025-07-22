package dev.kuku.vfl.fluent;

import dev.kuku.vfl.IVFL;
import dev.kuku.vfl.ThreadVFL;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Fluent API for ThreadVFL providing block operations with thread-local execution.
 * <p>
 * Usage examples:
 * - fluent.startBlock("myBlock").run(() -> {...})
 * - fluent.startBlock("myBlock").withMsg("Starting process").run(() -> {...})
 * - fluent.startBlock("myBlock").andCall(() -> computeValue()).call()
 * - fluent.startBlock("myBlock").withMsg("Processing").asAsync().run(() -> {...})
 * - fluent.startBlock("myBlock").andCall(() -> processData()).withEndMsg(r -> "Result: " + r).call()
 */
public class ThreadVFLFluent extends VFLFluent {

    private static ThreadVFLFluent instance;

    private ThreadVFLFluent() {
        //Passing no logger. instead we will override getLogger() to give it ThreadVFL instance
        super(null);
    }

    //Overriding get logger to pass current thread's logger. This function is used to get the logger before logging operations in parent class
    @Override
    protected IVFL getLogger() {
        return ThreadVFL.Get();
    }

    public static ThreadVFLFluent get() {
        if (instance == null) {
            instance = new ThreadVFLFluent();
        }
        return instance;
    }

    /**
     * Start a thread-local block operation
     */
    public ThreadBlockStep startBlock(String blockName) {
        return new ThreadBlockStep(blockName);
    }

    /**
     * Thread-local block operation step
     */
    public static class ThreadBlockStep {
        private final String blockName;

        private ThreadBlockStep(String blockName) {
            this.blockName = blockName;
        }

        /**
         * Add optional message to the block
         */
        public ThreadBlockWithMsgStep withMsg(String message) {
            return new ThreadBlockWithMsgStep(blockName, message);
        }

        /**
         * Run thread-local block operation without message
         */
        public void run(Runnable runnable) {
            ThreadVFL.Get().run(blockName, null, runnable);
        }

        /**
         * Start a thread-local call operation without message
         */
        public <R> ThreadCallStep<R> andCall(Callable<R> callable) {
            return new ThreadCallStep<>(blockName, null, callable);
        }

        /**
         * Make this thread-local block operation async
         */
        public AsyncThreadBlockStep asAsync() {
            return new AsyncThreadBlockStep(blockName);
        }
    }

    /**
     * Thread-local block step with message
     */
    public static class ThreadBlockWithMsgStep {
        private final String blockName;
        private final String message;

        private ThreadBlockWithMsgStep(String blockName, String message) {
            this.blockName = blockName;
            this.message = message;
        }

        /**
         * Run thread-local block operation with message
         */
        public void run(Runnable runnable) {
            ThreadVFL.Get().run(blockName, message, runnable);
        }

        /**
         * Start a thread-local call operation with message
         */
        public <R> ThreadCallStep<R> andCall(Callable<R> callable) {
            return new ThreadCallStep<>(blockName, message, callable);
        }

        /**
         * Make this thread-local block operation async
         */
        public AsyncThreadBlockWithMsgStep asAsync() {
            return new AsyncThreadBlockWithMsgStep(blockName, message);
        }
    }

    /**
     * Thread-local call operation step
     */
    public static class ThreadCallStep<R> {
        private final String blockName;
        private final String message;
        private final Callable<R> callable;

        private ThreadCallStep(String blockName, String message, Callable<R> callable) {
            this.blockName = blockName;
            this.message = message;
            this.callable = callable;
        }

        /**
         * Execute thread-local call without end message transformation
         */
        public R call() {
            return ThreadVFL.Get().call(blockName, message, callable, Object::toString);
        }

        /**
         * Add end message transformation
         */
        public ThreadCallWithEndMsgStep<R> withEndMsg(Function<R, String> endMessageFn) {
            return new ThreadCallWithEndMsgStep<>(blockName, message, endMessageFn, callable);
        }
    }

    /**
     * Thread-local call step with end message transformation
     */
    public static class ThreadCallWithEndMsgStep<R> {
        private final String blockName;
        private final String message;
        private final Function<R, String> endMessageFn;
        private final Callable<R> callable;

        private ThreadCallWithEndMsgStep(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable) {
            this.blockName = blockName;
            this.message = message;
            this.endMessageFn = endMessageFn;
            this.callable = callable;
        }

        /**
         * Execute thread-local call with end message transformation
         */
        public R call() {
            return ThreadVFL.Get().call(blockName, message, callable, endMessageFn);
        }
    }

    /**
     * Async thread-local block step without message
     */
    public static class AsyncThreadBlockStep {
        private final String blockName;
        private Executor executor;

        private AsyncThreadBlockStep(String blockName) {
            this.blockName = blockName;
        }

        /**
         * Set custom executor (optional)
         */
        public AsyncThreadBlockStep withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Run async thread-local block operation
         */
        public CompletableFuture<Void> run(Runnable runnable) {
            if (executor != null) {
                return ThreadVFL.Get().runAsync(blockName, null, runnable, executor);
            } else {
                return ThreadVFL.Get().runAsync(blockName, null, runnable);
            }
        }

        /**
         * Start async thread-local call operation
         */
        public <R> AsyncThreadCallStep<R> andCall(Callable<R> callable) {
            return new AsyncThreadCallStep<>(blockName, null, callable, executor);
        }
    }

    /**
     * Async thread-local block step with message
     */
    public static class AsyncThreadBlockWithMsgStep {
        private final String blockName;
        private final String message;
        private Executor executor;

        private AsyncThreadBlockWithMsgStep(String blockName, String message) {
            this.blockName = blockName;
            this.message = message;
        }

        /**
         * Set custom executor (optional)
         */
        public AsyncThreadBlockWithMsgStep withExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Run async thread-local block operation with message
         */
        public CompletableFuture<Void> run(Runnable runnable) {
            if (executor != null) {
                return ThreadVFL.Get().runAsync(blockName, message, runnable, executor);
            } else {
                return ThreadVFL.Get().runAsync(blockName, message, runnable);
            }
        }

        /**
         * Start async thread-local call operation with message
         */
        public <R> AsyncThreadCallStep<R> andCall(Callable<R> callable) {
            return new AsyncThreadCallStep<>(blockName, message, callable, executor);
        }
    }

    /**
     * Async thread-local call operation step
     */
    public static class AsyncThreadCallStep<R> {
        private final String blockName;
        private final String message;
        private final Callable<R> callable;
        private final Executor executor;

        private AsyncThreadCallStep(String blockName, String message, Callable<R> callable, Executor executor) {
            this.blockName = blockName;
            this.message = message;
            this.callable = callable;
            this.executor = executor;
        }

        /**
         * Execute async thread-local call without end message transformation
         */
        public CompletableFuture<R> call() {
            if (executor != null) {
                return ThreadVFL.Get().callAsync(blockName, message, callable, Object::toString, executor);
            } else {
                return ThreadVFL.Get().callAsync(blockName, message, callable, Object::toString);
            }
        }

        /**
         * Add end message transformation for async thread-local call
         */
        public AsyncThreadCallWithEndMsgStep<R> withEndMsg(Function<R, String> endMessageFn) {
            return new AsyncThreadCallWithEndMsgStep<>(blockName, message, endMessageFn, callable, executor);
        }
    }

    /**
     * Async thread-local call step with end message transformation
     */
    public static class AsyncThreadCallWithEndMsgStep<R> {
        private final String blockName;
        private final String message;
        private final Function<R, String> endMessageFn;
        private final Callable<R> callable;
        private final Executor executor;

        private AsyncThreadCallWithEndMsgStep(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable, Executor executor) {
            this.blockName = blockName;
            this.message = message;
            this.endMessageFn = endMessageFn;
            this.callable = callable;
            this.executor = executor;
        }

        /**
         * Execute async thread-local call with end message transformation
         */
        public CompletableFuture<R> call() {
            if (executor != null) {
                return ThreadVFL.Get().callAsync(blockName, message, callable, endMessageFn, executor);
            } else {
                return ThreadVFL.Get().callAsync(blockName, message, callable, endMessageFn);
            }
        }
    }
}