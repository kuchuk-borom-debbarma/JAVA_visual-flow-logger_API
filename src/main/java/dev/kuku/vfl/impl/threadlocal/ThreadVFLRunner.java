package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.core.vfl_abstracts.runner.VFLCallableRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.getThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.trimId;

@Slf4j
public final class ThreadVFLRunner extends VFLCallableRunner {

    /**
     * Singleton instance of ThreadVFLRunner.
     * Using singleton pattern ensures consistent behavior across the application
     * while maintaining thread safety through ThreadLocal storage.
     */
    private static final ThreadVFLRunner INSTANCE = new ThreadVFLRunner();

    /**
     * Private constructor to enforce singleton pattern.
     * External code should use the static factory methods instead of direct instantiation.
     */
    private ThreadVFLRunner() {
        // Private constructor for singleton pattern
    }

    /**
     * Validates parameters for startVFL method calls.
     *
     * @param blockName  the block name to validate
     * @param buffer     the buffer to validate
     * @param executable the function or runnable to validate
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private static void validateStartVFLParameters(String blockName, VFLBuffer buffer, Object executable) {
        if (blockName == null || blockName.trim().isEmpty()) {
            throw new IllegalArgumentException("Block name cannot be null or empty");
        }
        if (buffer == null) {
            throw new IllegalArgumentException("VFL buffer cannot be null");
        }
        if (executable == null) {
            throw new IllegalArgumentException("Function/Runnable cannot be null");
        }
    }

    /**
     * Validates parameters for startEventListenerLogger method calls.
     *
     * @param eventListenerName   the event listener name to validate
     * @param buffer              the buffer to validate
     * @param eventPublisherBlock the event publisher block to validate
     * @param runnable            the runnable to validate
     * @throws IllegalArgumentException if any parameter is invalid
     */
    private static void validateEventListenerParameters(
            String eventListenerName,
            VFLBuffer buffer,
            EventPublisherBlock eventPublisherBlock,
            Runnable runnable) {

        if (eventListenerName == null || eventListenerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Event listener name cannot be null or empty");
        }
        if (buffer == null) {
            throw new IllegalArgumentException("VFL buffer cannot be null");
        }
        if (eventPublisherBlock == null) {
            throw new IllegalArgumentException("Event publisher block cannot be null");
        }
        if (runnable == null) {
            throw new IllegalArgumentException("Runnable cannot be null");
        }
    }

    // ==================== STATIC METHODS ====================

    /**
     * Static version of startVFL method for executing functions within VFL logging context.
     *
     * <p>This static method provides convenient access to VFL functionality without requiring
     * direct instance management. It delegates to the singleton instance while providing
     * the same functionality and error handling.
     *
     * @param <R>       the return type of the function
     * @param blockName descriptive name for the logging block
     * @param buffer    the VFL buffer where log entries will be stored
     * @param function  the function to execute within the logging context
     * @return the result returned by the executed function
     * @throws IllegalArgumentException if blockName is null or empty, buffer is null, or function is null
     * @throws RuntimeException         if an error occurs during VFL execution (wraps underlying exceptions)
     */
    public static <R> R StartVFL(String blockName, VFLBuffer buffer, Supplier<R> function) {
        return INSTANCE.startVFL(blockName, buffer, function);
    }

    /**
     * Static version of startVFL method for executing runnables within VFL logging context.
     *
     * <p>This static method provides convenient access to VFL functionality without requiring
     * direct instance management. It delegates to the singleton instance while providing
     * the same functionality and error handling.
     *
     * @param blockName descriptive name for the logging block
     * @param buffer    the VFL buffer where log entries will be stored
     * @param runnable  the runnable to execute within the logging context
     * @throws IllegalArgumentException if blockName is null or empty, buffer is null, or runnable is null
     * @throws RuntimeException         if an error occurs during VFL execution (wraps underlying exceptions)
     */
    public static void StartVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
        INSTANCE.startVFL(blockName, buffer, runnable);
    }

    /**
     * Static version of startEventListenerLogger method for event-driven logging scenarios.
     *
     * <p>This static method provides convenient access to event listener logging functionality
     * without requiring direct instance management. It delegates to the singleton instance
     * while providing the same functionality and error handling.
     *
     * @param eventListenerName   descriptive name for the event listener
     * @param eventStartMessage   optional message logged when the event listener starts
     * @param buffer              the VFL buffer where log entries will be stored
     * @param eventPublisherBlock context information from the event publisher
     * @param runnable            the runnable to execute within the event listener context
     * @throws IllegalArgumentException if any required parameter is null or eventListenerName is empty
     * @throws RuntimeException         if an error occurs during event listener execution
     */
    public static void StartEventListenerLogger(
            String eventListenerName,
            String eventStartMessage,
            VFLBuffer buffer,
            EventPublisherBlock eventPublisherBlock,
            Runnable runnable) {

        INSTANCE.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventPublisherBlock, runnable);
    }

    // ==================== INSTANCE METHODS ====================

    /**
     * Starts a VFL logging block and executes the provided function within its context.
     *
     * <p>This method creates a root logging context, initializes the ThreadLocal logger stack,
     * executes the function, and ensures proper cleanup. The function's return value is
     * preserved and returned to the caller.
     *
     * <p>The logging block automatically captures the start and end of execution,
     * along with any logs generated by the function during its execution.
     *
     * @param <R>       the return type of the function
     * @param blockName descriptive name for the logging block
     * @param buffer    the VFL buffer where log entries will be stored
     * @param function  the function to execute within the logging context
     * @return the result returned by the executed function
     * @throws IllegalArgumentException if blockName is null or empty, buffer is null, or function is null
     * @throws RuntimeException         if an error occurs during VFL execution (wraps underlying exceptions)
     */
    public <R> R startVFL(String blockName, VFLBuffer buffer, Supplier<R> function) {
        validateStartVFLParameters(blockName, buffer, function);

        try {
            return super.startVFL(blockName, buffer, function);
        } catch (Exception e) {
            log.error("Failed to execute VFL block '{}': {}", blockName, e.getMessage(), e);
            throw new RuntimeException("VFL execution failed for block: " + blockName, e);
        }
    }

    /**
     * Starts a VFL logging block and executes the provided runnable within its context.
     *
     * <p>This method creates a root logging context, initializes the ThreadLocal logger stack,
     * executes the runnable, and ensures proper cleanup. This is the void-returning variant
     * of the VFL execution method.
     *
     * <p>The logging block automatically captures the start and end of execution,
     * along with any logs generated by the runnable during its execution.
     *
     * @param blockName descriptive name for the logging block
     * @param buffer    the VFL buffer where log entries will be stored
     * @param runnable  the runnable to execute within the logging context
     * @throws IllegalArgumentException if blockName is null or empty, buffer is null, or runnable is null
     * @throws RuntimeException         if an error occurs during VFL execution (wraps underlying exceptions)
     */
    public void startVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
        validateStartVFLParameters(blockName, buffer, runnable);

        try {
            super.startVFL(blockName, buffer, runnable);
        } catch (Exception e) {
            log.error("Failed to execute VFL block '{}': {}", blockName, e.getMessage(), e);
            throw new RuntimeException("VFL execution failed for block: " + blockName, e);
        }
    }

    /**
     * Starts an event listener logger and executes the provided runnable within its context.
     *
     * <p>This method handles event-driven logging scenarios where operations are triggered
     * by events rather than direct method calls. It intelligently manages the logger stack
     * based on whether the event listener is executing on the same thread as the event
     * publisher or on a different thread.
     *
     * <h4>Threading Behavior:</h4>
     * <ul>
     *   <li><strong>Same-thread execution:</strong> If called on a thread that already has
     *       a logger stack, the event listener logger is pushed onto the existing stack</li>
     *   <li><strong>Cross-thread execution:</strong> If called on a thread without a logger
     *       stack, a new stack is created and initialized with the event listener logger</li>
     * </ul>
     *
     * @param eventListenerName   descriptive name for the event listener
     * @param eventStartMessage   optional message logged when the event listener starts
     * @param buffer              the VFL buffer where log entries will be stored
     * @param eventPublisherBlock context information from the event publisher
     * @param runnable            the runnable to execute within the event listener context
     * @throws IllegalArgumentException if any required parameter is null or eventListenerName is empty
     * @throws RuntimeException         if an error occurs during event listener execution
     */
    public void startEventListenerLogger(
            String eventListenerName,
            String eventStartMessage,
            VFLBuffer buffer,
            EventPublisherBlock eventPublisherBlock,
            Runnable runnable) {

        validateEventListenerParameters(eventListenerName, buffer, eventPublisherBlock, runnable);

        try {
            super.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventPublisherBlock, runnable);
        } catch (Exception e) {
            log.error("Failed to execute event listener '{}': {}", eventListenerName, e.getMessage(), e);
            throw new RuntimeException("Event listener execution failed: " + eventListenerName, e);
        }
    }

    /**
     * Creates and initializes a root logger with a new ThreadLocal stack.
     *
     * <p>This method is called by the parent VFLCallableRunner when starting a new VFL block.
     * It creates a ThreadVFL instance, initializes a new logger stack, and sets up the
     * ThreadLocal storage for the current thread.
     *
     * <p>The root logger serves as the foundation for all subsequent sub-block logging
     * operations within the current execution thread.
     *
     * @param rootContext the execution context for the root logger
     * @return the created root logger instance
     * @throws IllegalArgumentException if rootContext is null
     */
    @Override
    protected VFL createRootLogger(VFLBlockContext rootContext) {
        if (rootContext == null) {
            throw new IllegalArgumentException("Root context cannot be null");
        }

        ThreadVFL rootLogger = new ThreadVFL(rootContext);
        String threadInfo = getThreadInfo();

        // Initialize the ThreadLocal logger stack for this thread
        Stack<ThreadVFL> loggerStack = new Stack<>();
        loggerStack.push(rootLogger);
        ThreadVFL.LOGGER_STACK.set(loggerStack);

        log.debug("CREATE ROOT: Created root logger '{}' and initialized stack {} - Stack size: {}",
                trimId(rootLogger.loggerContext.blockInfo.getId()),
                threadInfo,
                loggerStack.size());

        return rootLogger;
    }

    /**
     * Creates and manages an event listener logger with intelligent thread handling.
     *
     * <p>This method implements the core logic for event-driven logging, determining
     * whether to create a new logger stack or use an existing one based on the current
     * thread's state. This flexibility allows the system to handle both synchronous
     * and asynchronous event processing scenarios.
     *
     * <h4>Implementation Logic:</h4>
     * <ol>
     *   <li>Check if the current thread already has a logger stack</li>
     *   <li>If no stack exists, create a new one (cross-thread scenario)</li>
     *   <li>If a stack exists, add to it (same-thread scenario)</li>
     *   <li>Return the active logger from the top of the stack</li>
     * </ol>
     *
     * @param eventListenerContext the execution context for the event listener
     * @return the event listener logger instance
     * @throws IllegalArgumentException if eventListenerContext is null
     */
    @Override
    protected VFL createEventListenerLogger(VFLBlockContext eventListenerContext) {
        if (eventListenerContext == null) {
            throw new IllegalArgumentException("Event listener context cannot be null");
        }

        ThreadVFL eventLogger = new ThreadVFL(eventListenerContext);
        String threadInfo = getThreadInfo();
        String loggerId = trimId(eventLogger.loggerContext.blockInfo.getId());

        Stack<ThreadVFL> currentStack = ThreadVFL.LOGGER_STACK.get();

        if (currentStack == null) {
            // Cross-thread scenario: Create new stack for this thread
            Stack<ThreadVFL> newStack = new Stack<>();
            ThreadVFL.LOGGER_STACK.set(newStack);
            newStack.push(eventLogger);

            log.debug("CREATE EVENT: Created new stack for event listener '{}' {} - Stack size: {}",
                    loggerId, threadInfo, newStack.size());
        } else {
            // Same-thread scenario: Add to existing stack
            currentStack.push(eventLogger);

            log.debug("PUSH EVENT: Added event listener '{}' to existing stack {} - Stack size: {}",
                    loggerId, threadInfo, currentStack.size());
        }

        // Return the logger from the top of the stack (which is now our event logger)
        return ThreadVFL.getCurrentLogger();
    }
}