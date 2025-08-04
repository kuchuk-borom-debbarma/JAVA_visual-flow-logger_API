package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

/**
 * ThreadLocal-based implementation of VFLCallable that provides hierarchical logging
 * within synchronous thread execution contexts.
 *
 * <p>This implementation leverages the synchronous nature of thread execution to maintain
 * a stack-based hierarchy of loggers using ThreadLocal storage. Each sub-block execution
 * creates a new logger instance that is pushed onto the current thread's logger stack,
 * ensuring proper context isolation and automatic cleanup when sub-blocks complete.
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Stack-based Logger Hierarchy:</strong> Uses a ThreadLocal Stack to maintain
 *       the current logger context, with the top of the stack always representing the
 *       active logging context</li>
 *   <li><strong>Automatic Context Management:</strong> Sub-block loggers are automatically
 *       pushed and popped from the stack during execution</li>
 *   <li><strong>Thread Safety:</strong> Each thread maintains its own logger stack,
 *       preventing cross-thread interference</li>
 *   <li><strong>Async Support:</strong> Properly initializes logger context for
 *       asynchronous operations running on separate threads</li>
 * </ul>
 *
 * <h3>Usage Pattern:</h3>
 * <pre>{@code
 * // Initialize root logger for the thread
 * ThreadVFL.initializeForCurrentThread(rootContext);
 *
 * // Use the logger - sub-blocks will automatically manage the stack
 * ThreadVFL logger = ThreadVFL.getCurrentLogger();
 * logger.supply("SubBlock", () -> {
 *     // This code runs with a new logger pushed onto the stack
 *     return someOperation();
 * });
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe as each thread maintains
 * its own independent logger stack via ThreadLocal storage.
 *
 * @author Kuchuk Boram Debbarma
 * @see VFLCallable
 * @see VFLBlockContext
 * @since 1.0
 */
@Slf4j
public class ThreadVFL extends VFLCallable {

    /**
     * ThreadLocal storage for maintaining the logger stack hierarchy within each thread.
     * The stack ensures that nested sub-block executions maintain proper logging context
     * with the most recent (deepest) logger always at the top of the stack.
     */
    static final ThreadLocal<Stack<ThreadVFL>> LOGGER_STACK = new ThreadLocal<>();

    /**
     * The execution context associated with this specific logger instance.
     * Contains block information, buffer references, and current log sequence state.
     */
    final VFLBlockContext loggerContext;

    /**
     * Constructs a new ThreadVFL instance with the specified logging context.
     *
     * <p>This constructor is package-private as ThreadVFL instances should be created
     * through the framework's initialization methods rather than direct instantiation.
     *
     * @param loggerContext the execution context for this logger instance
     * @throws IllegalArgumentException if loggerContext is null
     */
    ThreadVFL(VFLBlockContext loggerContext) {
        if (loggerContext == null) {
            throw new IllegalArgumentException("Logger context cannot be null");
        }
        this.loggerContext = loggerContext;
    }

    /**
     * Retrieves the currently active logger for the current thread.
     *
     * <p>Due to the synchronous nature of thread execution, this method always returns
     * the logger associated with the currently executing block context. The logger
     * represents the deepest nested sub-block that is currently active.
     *
     * @return the active ThreadVFL logger instance for the current thread
     * @throws IllegalStateException if no logger stack has been initialized for the current thread
     * @throws IllegalStateException if the logger stack is empty (no active logger context)
     */
    public static ThreadVFL getCurrentLogger() {
        Stack<ThreadVFL> stack = LOGGER_STACK.get();

        if (stack == null) {
            throw new IllegalStateException(
                    "No logger stack initialized for current thread. " +
                            "Ensure ThreadVFL.initializeForCurrentThread() was called before using the logger."
            );
        }

        if (stack.isEmpty()) {
            throw new IllegalStateException(
                    "Logger stack is empty. No active logger context available."
            );
        }

        return stack.peek();
    }

    /**
     * Initializes the logger stack for the current thread with a root logger instance.
     *
     * <p>This method must be called before any logging operations can be performed
     * on the current thread. It establishes the root logging context and prepares
     * the ThreadLocal storage for subsequent sub-block operations.
     *
     * @param rootContext the root execution context to initialize the logger stack with
     * @throws IllegalArgumentException if rootContext is null
     * @throws IllegalStateException    if a logger stack is already initialized for the current thread
     */
    public static void initializeForCurrentThread(VFLBlockContext rootContext) {
        if (rootContext == null) {
            throw new IllegalArgumentException("Root context cannot be null");
        }

        if (LOGGER_STACK.get() != null) {
            throw new IllegalStateException(
                    "Logger stack already initialized for current thread. " +
                            "Call clearCurrentThread() first if reinitialization is needed."
            );
        }

        Stack<ThreadVFL> stack = new Stack<>();
        stack.push(new ThreadVFL(rootContext));
        LOGGER_STACK.set(stack);

        log.debug("Initialized ThreadVFL stack for thread: {}", Thread.currentThread().getName());
    }

    /**
     * Clears the logger stack for the current thread, releasing ThreadLocal resources.
     *
     * <p>This method should be called when thread execution is complete to prevent
     * memory leaks associated with ThreadLocal storage. It's particularly important
     * in environments where threads are reused (e.g., thread pools).
     */
    public static void clearCurrentThread() {
        LOGGER_STACK.remove();
        log.debug("Cleared ThreadVFL stack for thread: {}", Thread.currentThread().getName());
    }

    /**
     * Returns the current depth of the logger stack for diagnostic purposes.
     *
     * @return the number of nested logger contexts in the current thread's stack
     * @throws IllegalStateException if no logger stack is initialized for the current thread
     */
    public static int getStackDepth() {
        Stack<ThreadVFL> stack = LOGGER_STACK.get();
        if (stack == null) {
            throw new IllegalStateException("No logger stack initialized for current thread");
        }
        return stack.size();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the currently active logger from the top of the thread's logger stack.
     * Since thread execution is synchronous, the top of the stack always represents
     * the logger for the currently executing sub-block context.
     */
    @Override
    protected VFLCallable getSubBlockLogger() {
        return getCurrentLogger();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new ThreadVFL logger instance for the sub-block and pushes it onto
     * the current thread's logger stack. This ensures that subsequent logging operations
     * within the sub-block use the appropriate context while maintaining the hierarchical
     * relationship with parent blocks.
     *
     * <p>The new logger inherits the buffer reference from the current execution context
     * but operates within the scope of the newly created sub-block.
     *
     * @param executionContext the execution context at the time of sub-block creation
     * @param createdSubBlock  the sub-block that was created for this execution
     * @param subBlockStartLog the start log entry associated with the sub-block
     */
    @Override
    protected void initializeSubBlockInImplementation(
            VFLBlockContext executionContext,
            Block createdSubBlock,
            SubBlockStartLog subBlockStartLog) {

        // Create new logger context for the sub-block, inheriting the buffer
        VFLBlockContext subBlockContext = new VFLBlockContext(
                createdSubBlock,
                getCurrentLogger().getContext().buffer
        );

        // Create and push the new logger onto the stack
        ThreadVFL subBlockLogger = new ThreadVFL(subBlockContext);
        LOGGER_STACK.get().push(subBlockLogger);

        log.trace("Pushed sub-block logger onto stack. Stack depth: {}", LOGGER_STACK.get().size());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Initializes a new logger stack for the asynchronous thread and establishes
     * the sub-block context. Since the async execution runs on a separate thread,
     * it requires its own ThreadLocal logger stack to be created and initialized
     * with the appropriate sub-block context.
     *
     * <p><strong>Important:</strong> This method is called within the executor thread,
     * not the original calling thread. It sets up the necessary ThreadLocal state
     * for the async operation to have proper logging context.
     *
     * @param subBlock         the sub-block that will execute asynchronously
     * @param subBlockStartLog the start log entry associated with the async sub-block
     */
    @Override
    protected void setupAsyncSubBlockContext(Block subBlock, Log subBlockStartLog) {
        // Create new stack for the async thread
        Stack<ThreadVFL> asyncStack = new Stack<>();

        // Create sub-block context - note: we need to get buffer from a different source
        // since we're on a different thread. This is a limitation that should be addressed
        // by passing the buffer reference through the async setup.
        VFLBlockContext asyncContext = new VFLBlockContext(subBlock, loggerContext.buffer);

        // Initialize the async thread's logger stack
        asyncStack.push(new ThreadVFL(asyncContext));
        LOGGER_STACK.set(asyncStack);

        log.debug("Initialized async ThreadVFL stack for thread: {}", Thread.currentThread().getName());
    }

    @Override
    public void close(String endMessage) {
        super.close(endMessage);
        LOGGER_STACK.get().pop();
        if (LOGGER_STACK.get().isEmpty()) {
            log.debug("Closing ThreadVFL stack for thread: {}-{}", Thread.currentThread().getName(), Thread.currentThread().threadId());
            LOGGER_STACK.remove();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the execution context associated with the currently active logger.
     * This context contains the block information, buffer references, and current
     * log sequence state for the active logging scope.
     */
    @Override
    protected VFLBlockContext getContext() {
        return getCurrentLogger().loggerContext;
    }
}