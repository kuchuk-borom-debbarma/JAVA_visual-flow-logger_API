package dev.kuku.vfl;

import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class ThreadVFL extends VFL implements IThreadVFL {
    static final ThreadLocal<Stack<ThreadVFL>> THREAD_VFL_STACK = new ThreadLocal<>();

    /**
     * Setup thread's logger stack and then call the callable. Cleans thread variable after operation is complete.
     */
    static <R> R SetupNewThreadLoggerStackAndCall(ThreadVFL logger, Callable<R> callable) {
        if (THREAD_VFL_STACK.get() != null) {
            throw new IllegalStateException("Failed to setup logger stack in thread" + Thread.currentThread().getName() + ". Logger stack already available");
        }
        Stack<ThreadVFL> loggerStack = new Stack<>();
        loggerStack.push(logger);
        THREAD_VFL_STACK.set(loggerStack);
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            THREAD_VFL_STACK.remove();
        }
    }

    public static ThreadVFL Get() {
        var current = THREAD_VFL_STACK.get();
        if (current == null || current.empty()) {
            throw new NullPointerException("ThreadLocal VFL has not been initialized. Please use " + ThreadVFLRunner.class.getSimpleName() + " to start a new root flow");
        }
        //Get the latest logger in the stack.
        return current.peek();
    }

    ThreadVFL(VFLBlockContext context) {
        super(context);
    }

    @Override
    public void closeBlock(String endMessage) {
        super.closeBlock(endMessage);
        //Pop the logger from the thread stack.
        var current = ThreadVFL.Get();
        if (current != this) {
            throw new IllegalStateException("Current logger is not the latest logger in stack");
        }
        THREAD_VFL_STACK.get().pop();
    }

    private static <R> R ProcessCallableInCurrentThreadLogger(Callable<R> callable, Function<R, String> endMsgFn) {
        //Get the latest pushed logger and pass it to block function handler
        ThreadVFL subLogger = ThreadVFL.Get();
        return BlockHelper.CallFnForLogger(callable, endMsgFn, null, subLogger);
    }

    @Override
    public <R> void run(String blockName, String startMessage, Runnable runnable) {
        ensureBlockStarted();
        BlockHelper.SetupSubBlockStart(blockName, startMessage, true, blockContext, ThreadVFL::new, loggerAndBlockLogData -> ProcessCallableInCurrentThreadLogger(() -> runnable, null));
    }

    @Override
    public <R> CompletableFuture<Void> runAsync(String blockName, String startMessage, Runnable runnable, Executor executor) {
        ensureBlockStarted();
        var startedResult = BlockHelper.SetupSubBlockStart(blockName, startMessage, false, blockContext
                , ThreadVFL::new, null);
        return CompletableFuture.runAsync(() -> ProcessCallableInCurrentThreadLogger(() -> {
            runnable.run();
            return null;
        }, null), executor);
    }

    @Override
    public <R> CompletableFuture<Void> runAsync(String blockName, String startMessage, Runnable runnable) {
        ensureBlockStarted();
        var startedResult = BlockHelper.SetupSubBlockStart(blockName, startMessage, false, blockContext
                , ThreadVFL::new, null);
        return CompletableFuture.runAsync(() -> ProcessCallableInCurrentThreadLogger(() -> {
            runnable.run();
            return null;
        }, null));
    }

    @Override
    public <R> R call(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMsgFn) {
        //Ensure that the block has started
        ensureBlockStarted();
        //Create and push log&block data to buffer, move forward if desired and returns them.
        BlockHelper.SetupSubBlockStart(blockName, startMessage, true, blockContext, ThreadVFL::new,
                loggerAndBlockLogData -> THREAD_VFL_STACK.get().push((ThreadVFL) loggerAndBlockLogData.logger()));
        return ProcessCallableInCurrentThreadLogger(callable, endMsgFn);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor) {
        ensureBlockStarted();
        //Setup start block
        var startResult = BlockHelper.SetupSubBlockStart(blockName, message, false, blockContext, ThreadVFL::new, null);
        //return a completable future within which we setup thread logger stack and then use ProcessCallableInCurrentThreadLogger to execute the callable.
        return CompletableFuture.supplyAsync(() -> ThreadVFL.SetupNewThreadLoggerStackAndCall(
                (ThreadVFL) startResult.logger(), () -> ProcessCallableInCurrentThreadLogger(callable, endMsgFn)
        ), executor);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn) {
        return null;
    }
}