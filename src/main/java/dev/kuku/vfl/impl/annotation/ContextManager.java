package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

import static dev.kuku.vfl.core.helpers.Util.GetThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.TrimId;
import static dev.kuku.vfl.core.helpers.VFLFlowHelper.CreateBlockAndPush2Buffer;
import static dev.kuku.vfl.core.helpers.VFLFlowHelper.CreateLogAndPush2Buffer;

/**
 * Provides context that is persistent per thread and manages VFL context operations
 */
public class ContextManager {
    public static final Logger log = LoggerFactory.getLogger(ContextManager.class);

    public static final ThreadLocal<Stack<VFLBlockContext>> loggerCtxStack = new ThreadLocal<>();
    public static final ThreadLocal<SpawnedThreadContext> spawnedThreadContext = new ThreadLocal<>();
    public static VFLBuffer AnnotationBuffer;

    /**
     * Gets the current context from the stack
     */
    public static VFLBlockContext getCurrentContext() {
        Stack<VFLBlockContext> stack = loggerCtxStack.get();
        return stack != null && !stack.isEmpty() ? stack.peek() : null;
    }

    /**
     * Checks if the current thread has an active context stack
     */
    public static boolean hasActiveContext() {
        return loggerCtxStack.get() != null;
    }    public static VFL logger = new VFL() {
        @Override
        protected VFLBlockContext getContext() {
            //If attempting to log in a new thread with no logger context stack, create a new sub block call.
            //Users MUST use VFLFutures for this operation because VFLFutures clean up the left-over lambdas
            if (!hasActiveContext() && isSpawnedThread()) {
                startSubBlockFromSpawnedThreadContext(Thread.currentThread().getName() + "_" + Thread.currentThread().getId());
            }
            return getCurrentContext();
        }
    };

    /**
     * Checks if the current thread is a spawned thread with context
     */
    public static boolean isSpawnedThread() {
        return spawnedThreadContext.get() != null;
    }

    /**
     * Initializes a new context stack for the current thread
     */
    private static void initializeContextStack() {
        loggerCtxStack.set(new Stack<>());
    }

    /**
     * Pushes a new context to the stack
     */
    private static void pushContext(VFLBlockContext context) {
        if (loggerCtxStack.get() == null) {
            initializeContextStack();
        }
        loggerCtxStack.get().push(context);
    }

    /**
     * Creates and starts a root block context
     */
    public static void startRootBlock(String blockName) {
        Block rootBlock = CreateBlockAndPush2Buffer(blockName, null, AnnotationBuffer);
        VFLBlockContext rootContext = new VFLBlockContext(rootBlock, AnnotationBuffer);

        initializeContextStack();
        pushContext(rootContext);
        spawnedThreadContext.remove(); //Remove left over spawnedThreadContext if any. Should always be clean but still
        logger.ensureBlockStarted();

        log.debug("[VFL] Started root block: {}-{} in thread {}",
                rootBlock.getBlockName(), TrimId(rootBlock.getId()), GetThreadInfo());
    }

    public static void startSubBlockFromSpawnedThreadContext(String blockName) {
        SpawnedThreadContext callerData = spawnedThreadContext.get();
        log.debug("Starting sub block {} from spawned thread context {}-{}", blockName, callerData.parentContext().blockInfo.getBlockName(), Util.TrimId(callerData.parentContext().blockInfo.getId()));

        // Create sub block in new thread
        Block subBlockNewThread = CreateBlockAndPush2Buffer(
                blockName,
                callerData.parentContext().blockInfo.getId(),
                AnnotationBuffer
        );

        // Create sub block start log and add it to caller's log
        CreateLogAndPush2Buffer(
                callerData.parentContext().blockInfo.getId(),
                callerData.parentContext().currentLogId,
                null,
                subBlockNewThread.getId(),
                callerData.startType(),
                AnnotationBuffer
        );

        // Create context for started sub block and push it to stack
        VFLBlockContext currentContext = new VFLBlockContext(subBlockNewThread, AnnotationBuffer);
        initializeContextStack();
        pushContext(currentContext);
        logger.ensureBlockStarted();

        log.debug("[VFL] Started spawned thread block: {}-{} in thread {}",
                subBlockNewThread.getBlockName(), TrimId(subBlockNewThread.getId()), GetThreadInfo());
    }

    /**
     * Creates and starts a sub block context
     */
    public static void startSubBlock(String blockName) {
        VFLBlockContext parentContext = getCurrentContext();

        // Create sub block
        assert parentContext != null;
        Block primarySubBlockStart = CreateBlockAndPush2Buffer(
                blockName,
                parentContext.blockInfo.getId(),
                AnnotationBuffer
        );

        // Create sub block start log and add it to caller's log
        SubBlockStartLog subBlockStartLog = CreateLogAndPush2Buffer(
                parentContext.blockInfo.getId(),
                parentContext.currentLogId,
                null,
                primarySubBlockStart.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                AnnotationBuffer
        );

        // Move forward the flow
        parentContext.currentLogId = subBlockStartLog.getId();

        // Create context for started sub block and push it to stack
        VFLBlockContext currentContext = new VFLBlockContext(primarySubBlockStart, AnnotationBuffer);
        pushContext(currentContext);
        logger.ensureBlockStarted();

        log.debug("[VFL] Started sub block: {}-{} in thread {}",
                primarySubBlockStart.getBlockName(), TrimId(primarySubBlockStart.getId()), GetThreadInfo());
    }

    /**
     * Logs an exception for the current context
     */
    public static void logException(Throwable exception) {
        VFLBlockContext currentCtx = getCurrentContext();
        if (currentCtx != null && exception != null) {
            logger.error(String.format("Exception : %s %s-%s in thread %s",
                    exception.getClass() + "_" + exception.getMessage(),
                    currentCtx.blockInfo.getBlockName(),
                    TrimId(currentCtx.blockInfo.getId()),
                    GetThreadInfo()));
        }
    }

    /**
     * Closes the current context and performs cleanup
     */
    public static void closeCurrentContext(Object returnValue) {
        // Close the logger context
        logger.close("Returning " + returnValue);

        // Pop the context
        Stack<VFLBlockContext> stack = loggerCtxStack.get();
        if (stack != null && !stack.isEmpty()) {
            VFLBlockContext poppedContext = stack.pop();
            log.debug("[VFL] Popped : {}-{} in thread {}",
                    poppedContext.blockInfo.getBlockName(),
                    TrimId(poppedContext.blockInfo.getId()),
                    GetThreadInfo());

            // Clean up if stack is empty
            if (stack.isEmpty()) {
                cleanupThreadContext(poppedContext);
            }
        } else {
            log.warn("[VFL] Stack is empty or null when it shouldn't be");
        }
    }

    /**
     * Cleans up thread-local resources when the context stack is empty
     */
    private static void cleanupThreadContext(VFLBlockContext lastContext) {
        boolean isRootThread = !isSpawnedThread();

        if (isRootThread) {
            log.debug("[VFL] COMPLETE: Operation Flow complete for {}-{} in Thread {}",
                    lastContext.blockInfo.getBlockName(),
                    TrimId(lastContext.blockInfo.getId()),
                    GetThreadInfo());
            AnnotationBuffer.flushAndClose();
        }

        log.debug("[VFL] EMPTIED STACK N SPAWNED CONTEXT: in thread {}", GetThreadInfo());

        // Clean up thread-local resources
        loggerCtxStack.remove();
        spawnedThreadContext.remove();
    }

    /**
     * Checks if the current thread's context stack is empty
     */
    public static boolean isContextStackEmpty() {
        Stack<VFLBlockContext> stack = loggerCtxStack.get();
        return stack == null || stack.isEmpty();
    }




}