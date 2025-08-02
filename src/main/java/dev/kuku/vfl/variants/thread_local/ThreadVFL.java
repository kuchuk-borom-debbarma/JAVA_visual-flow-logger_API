package dev.kuku.vfl.variants.thread_local;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import dev.kuku.vfl.core.vfl_abstracts.runner.VFLCallableRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class ThreadVFL extends VFLCallable {
    private static final ThreadLocal<Stack<ThreadVFL>> loggerStack = new ThreadLocal<>();
    private final VFLBlockContext ctx;

    private ThreadVFL(VFLBlockContext context) {
        this.ctx = context;
    }

    /**
     * Get the current logger from the thread local stack
     */
    static ThreadVFL getCurrentLogger() {
        return loggerStack.get().peek();
    }

    private static String getThreadInfo() {
        Thread currentThread = Thread.currentThread();
        return String.format("[Thread: %s (ID: %d)]", currentThread.getName(), currentThread.threadId());
    }

    private static String trimId(String fullId) {
        if (fullId == null) return "null";
        String[] parts = fullId.split("-");
        return parts.length > 0 ? parts[parts.length - 1] : fullId;
    }

    // ==================== Static Logging Methods ====================

    /**
     * Log a message at INFO level
     */
    public static void Log(String message) {
        getCurrentLogger().log(message);
    }

    /**
     * Execute a function and log its result at INFO level
     */
    public static <R> R LogFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return getCurrentLogger().logFn(fn, messageSerializer);
    }

    /**
     * Log a message at WARN level
     */
    public static void Warn(String message) {
        getCurrentLogger().warn(message);
    }

    /**
     * Execute a function and log its result at WARN level
     */
    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer, Object... args) {
        return getCurrentLogger().warnFn(fn, messageSerializer, args);
    }

    /**
     * Log a message at ERROR level
     */
    public static void Error(String message) {
        getCurrentLogger().error(message);
    }

    /**
     * Execute a function and log its result at ERROR level
     */
    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer, Object... args) {
        return getCurrentLogger().errorFn(fn, messageSerializer, args);
    }

    // ==================== Static Block Operations ====================

    /**
     * Start a primary sub block
     */
    public static <R> R CallPrimarySubBlock(String blockName, String startMessage,
                                            Supplier<R> supplier, Function<R, String> endMessageSerializer, Object... args) {
        return getCurrentLogger().callPrimarySubBlock(blockName, startMessage, supplier, endMessageSerializer, args);
    }

    /**
     * Create a secondary sub block that joins back to main flow
     */
    public static <R> CompletableFuture<R> CallSecondaryJoiningBlock(String blockName, String startMessage,
                                                                     Supplier<R> supplier,
                                                                     Executor executor, Function<R, String> endMessageSerializer, Object... args) {
        return getCurrentLogger().callSecondaryJoiningBlock(blockName, startMessage, supplier, executor, endMessageSerializer, args);
    }

    /**
     * Create a secondary sub block that joins back to main flow (using default executor)
     */
    public static <R> CompletableFuture<R> CallSecondaryJoiningBlock(String blockName, String startMessage,
                                                                     Supplier<R> supplier, Function<R, String> endMessageSerializer, Object... args) {
        return getCurrentLogger().callSecondaryJoiningBlock(blockName, startMessage, supplier, null, endMessageSerializer, args);
    }

    /**
     * Create a secondary sub block that does not join back to main flow
     */
    public static CompletableFuture<Void> CallSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                       Runnable runnable, Executor executor) {
        return getCurrentLogger().callSecondaryNonJoiningBlock(blockName, startMessage, runnable, executor);
    }

    /**
     * Create a secondary sub block that does not join back to main flow (using default executor)
     */
    public static CompletableFuture<Void> CallSecondaryNonJoiningBlock(String blockName, String startMessage,
                                                                       Runnable runnable) {
        return getCurrentLogger().callSecondaryNonJoiningBlock(blockName, startMessage, runnable, null);
    }

    /**
     * Create an event publisher block
     */
    public static EventPublisherBlock CreateEventPublisherBlock(String branchName, String startMessage, Object... args) {
        return getCurrentLogger().createEventPublisherBlock(branchName, startMessage, args);
    }

    // ==================== Instance Methods (inherited from parent) ====================
    // These remain as instance methods and are used by the static methods above

    // ==================== Original ThreadVFL Logic ====================

    @Override
    protected void prepareLoggerAfterSubBlockStartDataInitializedAndPushed(VFLBlockContext parentBlockCtx, Block subBlock, SubBlockStartLog subBlockStartLog, LogTypeBlockStartEnum startType) {
        var subLoggerCtx = new VFLBlockContext(subBlock, parentBlockCtx.buffer);
        String threadInfo = getThreadInfo();

        // If sub logger data has been pushed to buffer, we must now create the respective logger and add it to stack
        if (startType == LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY) {
            //if primary operation was started then it's in main thread and a stack with proper parent will already exist
            ThreadVFL newLogger = new ThreadVFL(subLoggerCtx);
            ThreadVFL.loggerStack.get().push(newLogger);
            log.debug("PUSH: Added logger '{}' to existing stack {} - Stack size: {}", trimId(subLoggerCtx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());
            return;
        }

        //If its completable futures start then it will be in a new thread, the thread may or may not have existing stack(if thread pool fails to remove thread properly although it should)
        //Then we need to create a new stack and push it
        if (ThreadVFL.loggerStack.get() == null) {
            Stack<ThreadVFL> stack = new Stack<>();
            ThreadVFL newLogger = new ThreadVFL(subLoggerCtx);
            stack.push(newLogger);
            ThreadVFL.loggerStack.set(stack);
            log.debug("CREATE & PUSH: Created new stack and added logger '{}' {} - Stack size: {}", trimId(subLoggerCtx.blockInfo.getId()), threadInfo, stack.size());
        } else {
            ThreadVFL newLogger = new ThreadVFL(subLoggerCtx);
            ThreadVFL.loggerStack.get().push(newLogger);
            log.debug("PUSH: Added logger '{}' to existing stack {} - Stack size: {}", trimId(subLoggerCtx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());
        }
    }

    /*
    this method is used by super classes to get logger's instance.
    We are overriding it to give it the thread local's logger stack's latest instance.
    This allows us to define single instance that can be used across different scopes and it will always return the right stack.
     */
    @Override
    protected ThreadVFL getLogger() {
        return getCurrentLogger();
    }

    @Override
    protected VFLBlockContext getContext() {
        return getCurrentLogger().ctx;
    }

    @Override
    protected void close(String endMessage) {
        super.close(endMessage);
        String threadInfo = getThreadInfo();

        var poppedLogger = ThreadVFL.loggerStack.get().pop();
        log.debug("POP: Removed logger '{}' from stack {} - Remaining stack size: {}", trimId(poppedLogger.ctx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());

        if (ThreadVFL.loggerStack.get().isEmpty()) {
            ThreadVFL.loggerStack.remove();
            log.debug("REMOVE: Completely removed logger stack from {} - Stack cleaned up", threadInfo);
        }
    }


    public static class Runner extends VFLCallableRunner {
        private final static Runner INSTANCE = new Runner();

        public static <R> R StartVFL(String blockName, VFLBuffer buffer, Supplier<R> fn) {
            return INSTANCE.startVFL(blockName, buffer, fn);
        }

        public static void StartVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
            INSTANCE.startVFL(blockName, buffer, runnable);
        }

        public static void StartEventListenerLogger(String eventListenerName, String eventStartMessage, VFLBuffer buffer, EventPublisherBlock eventData, Runnable r) {
            INSTANCE.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventData, r);
        }

        @Override
        protected VFL createRootLogger(VFLBlockContext rootCtx) {
            var rootLogger = new ThreadVFL(rootCtx);
            String threadInfo = getThreadInfo();

            //Create logger stack
            Stack<ThreadVFL> stack = new Stack<>();
            stack.push(rootLogger);
            ThreadVFL.loggerStack.set(stack);
            log.debug("CREATE ROOT: Created root logger '{}' and initialized stack {} - Stack size: {}", trimId(rootLogger.ctx.blockInfo.getId()), threadInfo, stack.size());
            return rootLogger;
        }

        @Override
        protected VFL createEventListenerLogger(VFLBlockContext eventListenerCtx) {
            ThreadVFL eventListenerBlockLogger = new ThreadVFL(eventListenerCtx);
            String threadInfo = getThreadInfo();

            /*
             * Depending on the implementation of the event publisher and subscribe implementation this may or may not be running in a separate thread.
             *
             * If it's in the same thread as caller then it will be sequential in nature and thus can be added to the stack which pops once operation is complete.
             *
             * If it's running in a different thread the loggerStack should be null as secondaryBlockStart calls always clean up after they are done.
             */
            if (ThreadVFL.loggerStack.get() == null) {
                Stack<ThreadVFL> newStack = new Stack<>();
                ThreadVFL.loggerStack.set(newStack);
                newStack.push(eventListenerBlockLogger);
                log.debug("CREATE EVENT: Created new stack for event listener '{}' {} - Stack size: {}", trimId(eventListenerBlockLogger.ctx.blockInfo.getId()), threadInfo, newStack.size());
            } else {
                ThreadVFL.loggerStack.get().push(eventListenerBlockLogger);
                log.debug("PUSH EVENT: Added event listener '{}' to existing stack {} - Stack size: {}", trimId(eventListenerBlockLogger.ctx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());
            }

            return ThreadVFL.loggerStack.get().peek();
        }
    }
}