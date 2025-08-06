package dev.kuku.vfl.impl.threadlocal.logger;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

import static dev.kuku.vfl.core.helpers.Util.GetThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.TrimId;

@Slf4j
public class ThreadVFL extends VFLCallable {

    /**
     * ThreadLocal storage for maintaining the logger stack hierarchy within each thread.
     * The stack ensures that nested sub-block executions maintain proper logging context
     * with the most recent (deepest) logger always at the top of the stack.
     */
    public static final ThreadLocal<Stack<ThreadVFL>> LOGGER_STACK = new ThreadLocal<>();

    /**
     * The execution context associated with this specific logger instance.
     * Contains block information, buffer references, and current log sequence state.
     */
    public final VFLBlockContext loggerContext;

    /**
     * Constructs a new ThreadVFL instance with the specified logging context.
     *
     * <p>This constructor is package-private as ThreadVFL instances should be created
     * through the framework's initialization methods rather than direct instantiation.
     *
     * @param loggerContext the execution context for this logger instance
     * @throws IllegalArgumentException if loggerContext is null
     */
    public ThreadVFL(VFLBlockContext loggerContext) {
        String threadInfo = GetThreadInfo();
        log.trace("ThreadVFL constructor called {} - Context: {}",
                threadInfo, loggerContext != null ? "present" : "null");

        if (loggerContext == null) {
            log.error("ThreadVFL constructor validation failed {} - loggerContext is null", threadInfo);
            throw new IllegalArgumentException("Logger context cannot be null");
        }

        this.loggerContext = loggerContext;
        String loggerId = loggerContext.blockInfo != null ? TrimId(loggerContext.blockInfo.getId()) : "unknown";
        String blockName = loggerContext.blockInfo != null ? loggerContext.blockInfo.getBlockName() : "unknown";

        log.debug("ThreadVFL instance created {} - Logger ID: '{}' - Block: '{}' - Buffer: {}",
                threadInfo, loggerId, blockName,
                loggerContext.buffer != null ? "present" : "null");
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
        String threadInfo = GetThreadInfo();
        log.trace("getCurrentLogger() called {}", threadInfo);

        Stack<ThreadVFL> stack = LOGGER_STACK.get();
        if (stack == null) {
            log.warn("STACK MISSING: No logger stack found for thread {} - This indicates VFL context not initialized",
                    threadInfo);
            return null;
        }

        if (stack.isEmpty()) {
            log.error("EMPTY STACK: Logger stack is empty for thread {} - This should not happen", threadInfo);
            return null;
        }

        ThreadVFL currentLogger = stack.peek();
        String loggerId = currentLogger != null && currentLogger.loggerContext.blockInfo != null ?
                TrimId(currentLogger.loggerContext.blockInfo.getId()) : "unknown";
        String blockName = currentLogger != null && currentLogger.loggerContext.blockInfo != null ?
                currentLogger.loggerContext.blockInfo.getBlockName() : "unknown";

        log.trace("CURRENT LOGGER: Retrieved logger '{}' {} - Block: '{}' - Stack depth: {}",
                loggerId, threadInfo, blockName, stack.size());

        return currentLogger;
    }

    @Override
    public String toString() {
        String loggerId = loggerContext != null && loggerContext.blockInfo != null ?
                TrimId(loggerContext.blockInfo.getId()) : "null";
        String blockName = loggerContext != null && loggerContext.blockInfo != null ?
                loggerContext.blockInfo.getBlockName() : "null";
        return "ThreadVFL{loggerId='" + loggerId + "', blockName='" + blockName + "'}";
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
        String threadInfo = GetThreadInfo();
        log.trace("getSubBlockLogger() called {}", threadInfo);

        ThreadVFL subBlockLogger = getCurrentLogger();
        if (subBlockLogger != null) {
            String loggerId = TrimId(subBlockLogger.loggerContext.blockInfo.getId());
            log.trace("SUB-BLOCK LOGGER: Returning logger '{}' {} for sub-block execution",
                    loggerId, threadInfo);
        } else {
            log.error("SUB-BLOCK LOGGER: No current logger available {} - Sub-block execution may fail",
                    threadInfo);
        }

        return subBlockLogger;
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

        String threadInfo = GetThreadInfo();
        String subBlockId = createdSubBlock != null ? TrimId(createdSubBlock.getId()) : "unknown";
        String subBlockName = createdSubBlock != null ? createdSubBlock.getBlockName() : "unknown";

        log.debug("INIT SUB-BLOCK: Starting initialization {} - Sub-block: '{}' (ID: '{}')",
                threadInfo, subBlockName, subBlockId);

        // Validate current state
        Stack<ThreadVFL> currentStack = LOGGER_STACK.get();
        if (currentStack == null) {
            log.error("INIT SUB-BLOCK ERROR: No logger stack found {} - Cannot initialize sub-block '{}'",
                    threadInfo, subBlockName);
            throw new IllegalStateException("No logger stack available for sub-block initialization");
        }

        ThreadVFL currentLogger = getCurrentLogger();
        if (currentLogger == null) {
            log.error("INIT SUB-BLOCK ERROR: No current logger {} - Cannot initialize sub-block '{}'",
                    threadInfo, subBlockName);
            throw new IllegalStateException("No current logger available for sub-block initialization");
        }

        String parentLoggerId = TrimId(currentLogger.loggerContext.blockInfo.getId());
        String parentBlockName = currentLogger.loggerContext.blockInfo.getBlockName();
        int stackDepthBefore = currentStack.size();

        log.debug("SUB-BLOCK CONTEXT: Parent logger '{}' ('{}') {} - Stack depth: {} - Creating child context",
                parentLoggerId, parentBlockName, threadInfo, stackDepthBefore);

        // Create new logger context for the sub-block, inheriting the buffer
        VFLBlockContext subBlockContext = new VFLBlockContext(
                createdSubBlock,
                currentLogger.getContext().buffer
        );

        // Create and push the new logger onto the stack
        ThreadVFL subBlockLogger = new ThreadVFL(subBlockContext);
        currentStack.push(subBlockLogger);

        int stackDepthAfter = currentStack.size();

        log.info("SUB-BLOCK INITIALIZED: Created logger '{}' for sub-block '{}' {} - Parent: '{}' - Stack depth: {} -> {}",
                subBlockId, subBlockName, threadInfo, parentLoggerId, stackDepthBefore, stackDepthAfter);

        // Verify the operation
        ThreadVFL verifyLogger = getCurrentLogger();
        String verifyId = verifyLogger != null ? TrimId(verifyLogger.loggerContext.blockInfo.getId()) : "null";
        boolean isCorrectLogger = subBlockId.equals(verifyId);

        log.debug("SUB-BLOCK VERIFICATION: Expected logger '{}' - Actual logger '{}' - Match: {} {}",
                subBlockId, verifyId, isCorrectLogger, threadInfo);

        if (!isCorrectLogger) {
            log.error("SUB-BLOCK VERIFICATION FAILED: Logger mismatch {} - Expected: '{}' - Actual: '{}'",
                    threadInfo, subBlockId, verifyId);
        }
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
        String threadInfo = GetThreadInfo();
        String subBlockId = subBlock != null ? TrimId(subBlock.getId()) : "unknown";
        String subBlockName = subBlock != null ? subBlock.getBlockName() : "unknown";

        log.info("ASYNC SETUP: Starting async context setup {} - Sub-block: '{}' (ID: '{}')",
                threadInfo, subBlockName, subBlockId);

        // Check if there's already a stack (shouldn't be for truly async operations)
        Stack<ThreadVFL> existingStack = LOGGER_STACK.get();
        if (existingStack != null) {
            log.warn("ASYNC SETUP WARNING: Found existing stack {} - Size: {} - This may indicate nested async calls or thread reuse",
                    threadInfo, existingStack.size());
        }

        // Create new stack for the async thread
        Stack<ThreadVFL> asyncStack = new Stack<>();

        // Validate buffer availability
        if (loggerContext == null || loggerContext.buffer == null) {
            log.error("ASYNC SETUP ERROR: No buffer available {} - Logger context: {} - Buffer: {}",
                    threadInfo,
                    loggerContext != null ? "present" : "null",
                    "null");
            throw new IllegalStateException("No buffer available for async sub-block setup");
        }

        // Create sub-block context - inheriting buffer from the original context
        VFLBlockContext asyncContext = new VFLBlockContext(subBlock, loggerContext.buffer);

        log.debug("ASYNC CONTEXT: Created context for sub-block '{}' {} - Buffer inherited: {}",
                subBlockName, threadInfo, asyncContext.buffer != null ? "yes" : "no");

        // Initialize the async thread's logger stack
        ThreadVFL asyncLogger = new ThreadVFL(asyncContext);
        asyncStack.push(asyncLogger);
        LOGGER_STACK.set(asyncStack);

        log.info("ASYNC INITIALIZED: Created async stack {} - Logger ID: '{}' - Sub-block: '{}' - Stack depth: {}",
                threadInfo, subBlockId, subBlockName, asyncStack.size());

        // Verify the setup
        ThreadVFL verifyLogger = getCurrentLogger();
        String verifyId = verifyLogger != null ? TrimId(verifyLogger.loggerContext.blockInfo.getId()) : "null";
        boolean setupCorrect = subBlockId.equals(verifyId);

        log.debug("ASYNC VERIFICATION: Expected logger '{}' - Actual logger '{}' - Setup correct: {} {}",
                subBlockId, verifyId, setupCorrect, threadInfo);

        if (!setupCorrect) {
            log.error("ASYNC VERIFICATION FAILED: Logger setup incorrect {} - Expected: '{}' - Actual: '{}'",
                    threadInfo, subBlockId, verifyId);
        }
    }

    public void onClose(String endMessage) {
        String threadInfo = GetThreadInfo();
        String thisLoggerId = loggerContext != null && loggerContext.blockInfo != null ?
                TrimId(loggerContext.blockInfo.getId()) : "unknown";
        String thisBlockName = loggerContext != null && loggerContext.blockInfo != null ?
                loggerContext.blockInfo.getBlockName() : "unknown";

        log.debug("CLOSE START: Closing logger '{}' ('{}') {} - End message: '{}'",
                thisLoggerId, thisBlockName, threadInfo, endMessage);

        // Validate stack state before closing
        Stack<ThreadVFL> currentStack = LOGGER_STACK.get();
        if (currentStack == null) {
            log.error("CLOSE ERROR: No logger stack found {} - Cannot close logger '{}'",
                    threadInfo, thisLoggerId);
            throw new IllegalStateException("No logger stack available for closing logger: " + thisLoggerId);
        }

        if (currentStack.isEmpty()) {
            log.error("CLOSE ERROR: Empty logger stack {} - Cannot close logger '{}'",
                    threadInfo, thisLoggerId);
            throw new IllegalStateException("Logger stack is empty, cannot close logger: " + thisLoggerId);
        }

        int stackSizeBefore = currentStack.size();
        ThreadVFL topLogger = currentStack.peek();
        String topLoggerId = topLogger != null && topLogger.loggerContext.blockInfo != null ?
                TrimId(topLogger.loggerContext.blockInfo.getId()) : "unknown";

        log.debug("CLOSE VALIDATION: Stack size: {} - Top logger: '{}' - This logger: '{}' {} - Match: {}",
                stackSizeBefore, topLoggerId, thisLoggerId, threadInfo, topLoggerId.equals(thisLoggerId));

        // Call parent close method
        super.close(endMessage);

        // Remove from stack and validate
        ThreadVFL removedLogger = currentStack.pop();
        String removedLoggerId = removedLogger != null && removedLogger.loggerContext.blockInfo != null ?
                TrimId(removedLogger.loggerContext.blockInfo.getId()) : "unknown";

        if (removedLogger != this) {
            log.error("CLOSE CONSISTENCY ERROR: Removed logger '{}' is not the same as this logger '{}' {} - Stack corruption detected",
                    removedLoggerId, thisLoggerId, threadInfo);
            throw new IllegalStateException("Latest logger is NOT same as this logger - Expected: " +
                    thisLoggerId + ", Removed: " + removedLoggerId);
        }

        int stackSizeAfter = currentStack.size();

        log.info("LOGGER CLOSED: Removed logger '{}' ('{}') {} - Stack size: {} -> {}",
                removedLoggerId, thisBlockName, threadInfo, stackSizeBefore, stackSizeAfter);

        // Handle stack cleanup
        if (currentStack.isEmpty()) {
            log.info("STACK CLEANUP: Removing empty ThreadVFL stack {} - Thread execution complete", threadInfo);
            LOGGER_STACK.remove();
        } else {
            // Log the new current logger
            ThreadVFL newCurrentLogger = getCurrentLogger();
            String newCurrentId = newCurrentLogger != null && newCurrentLogger.loggerContext.blockInfo != null ?
                    TrimId(newCurrentLogger.loggerContext.blockInfo.getId()) : "unknown";
            String newCurrentBlock = newCurrentLogger != null && newCurrentLogger.loggerContext.blockInfo != null ?
                    newCurrentLogger.loggerContext.blockInfo.getBlockName() : "unknown";

            log.debug("STACK ACTIVE: Current logger is now '{}' ('{}') {} - Stack depth: {}",
                    newCurrentId, newCurrentBlock, threadInfo, stackSizeAfter);
        }
    }

    @Override
    public void close(String endMessage) {
        String threadInfo = GetThreadInfo();
        String loggerId = loggerContext != null && loggerContext.blockInfo != null ?
                TrimId(loggerContext.blockInfo.getId()) : "unknown";

        log.debug("CLOSE WRAPPER: close() called for logger '{}' {} - Delegating to onClose()",
                loggerId, threadInfo);
        onClose(endMessage);
        log.debug("CLOSE WRAPPER: close() completed for logger '{}' {}", loggerId, threadInfo);
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
        String threadInfo = GetThreadInfo();
        log.trace("getContext() called {}", threadInfo);

        ThreadVFL currentLogger = getCurrentLogger();
        if (currentLogger == null) {
            log.error("CONTEXT ERROR: No current logger available {} - Cannot provide context", threadInfo);
            return null;
        }

        VFLBlockContext context = currentLogger.loggerContext;
        String loggerId = context != null && context.blockInfo != null ?
                TrimId(context.blockInfo.getId()) : "unknown";
        String blockName = context != null && context.blockInfo != null ?
                context.blockInfo.getBlockName() : "unknown";

        log.trace("CONTEXT PROVIDED: Logger '{}' ('{}') {} - Buffer: {}",
                loggerId, blockName, threadInfo,
                context != null && context.buffer != null ? "present" : "null");

        return context;
    }
}
