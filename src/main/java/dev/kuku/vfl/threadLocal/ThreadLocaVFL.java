package dev.kuku.vfl.threadLocal;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public class ThreadLocaVFL extends VFL implements IThreadLocal {
    static final ThreadLocal<Stack<ThreadLocaVFL>> THREAD_VFL_STACK = new ThreadLocal<>();

    /// Used by runner to start a root logger
    static <R> R start(String blockName, VFLBuffer buffer, Callable<R> callable) {
        var current = THREAD_VFL_STACK.get();
        if (current != null) {
            throw new NullPointerException("Can't start root thread vfl logger. Already running an existing operation");
        }
        VFLBlockContext rootCtx = new VFLBlockContext(new BlockData(generateUID(), null, blockName), buffer);
        ThreadLocaVFL parentLogger = new ThreadLocaVFL(rootCtx);
        var threadStack = new Stack<ThreadLocaVFL>();
        threadStack.push(parentLogger);
        THREAD_VFL_STACK.set(threadStack);
        buffer.pushBlockToBuffer(rootCtx.blockInfo);
        R r;
        try {
            parentLogger.ensureBlockStarted();
            r = parentLogger.fnHandler(callable, null);
            if (!THREAD_VFL_STACK.get().isEmpty()) {
                throw new IllegalStateException("Stack logger still not empty after root operation is complete");
            }
        } finally {
            THREAD_VFL_STACK.remove();
        }
        return r;
    }

    public static ThreadLocaVFL get() {
        var current = THREAD_VFL_STACK.get();
        if (current == null || current.empty()) {
            throw new NullPointerException("ThreadLocal VFL has not been initialized. Please run " + ThreadLocaVFL.class + ".init(a,b) to start a root logger");
        }
        //Get the latest logger in the stack.
        return current.peek();
    }

    private ThreadLocaVFL(VFLBlockContext context) {
        super(context);
    }

    private BlockData createBlockDataAndPush(String id, String blockName) {
        var b = new BlockData(id, this.blockContext.blockInfo.getId(), blockName);
        blockContext.buffer.pushBlockToBuffer(b);
        return b;
    }

    @Override
    public void closeBlock(String endMessage) {
        super.closeBlock(endMessage);
        var current = ThreadLocaVFL.get();
        if (current != this) {
            throw new IllegalStateException("Current logger is not the latest logger in stack");
        }
        THREAD_VFL_STACK.get().pop();
    }

    private <R> R fnHandler(Callable<R> callable, Function<R, String> endMsgFn) {
        R result = null;
        ThreadLocaVFL subLogger = ThreadLocaVFL.get();
        try {
            result = callable.call();
        } catch (Exception e) {
            subLogger.error(String.format("Exception : %s - %s", e.getClass().getSimpleName(), e.getMessage()));
            throw new RuntimeException(e);
        } finally {
            String endMsg = null;
            if (endMsgFn != null) {
                try {
                    endMsg = endMsgFn.apply(result);
                } catch (Exception e) {
                    endMsg = String.format("Failed to process end message " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            }
            subLogger.closeBlock(endMsg);
        }
        return result;
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor) {
        ensureBlockStarted();
        String subBlockId = generateUID();
        var subBlockData = createBlockDataAndPush(subBlockId, blockName);
        var createdLog = createLogAndPush(VflLogType.SUB_BLOCK_START, message, subBlockId);
        var current = ThreadLocaVFL.get();
        return CompletableFuture.supplyAsync(() -> {
            //Create a stack with proper sub block lgoger on the executing thread.
            try {
                var ctx = new VFLBlockContext(subBlockData, current.blockContext.buffer);
                var subLogger = new ThreadLocaVFL(ctx);
                var threadLoggerStack = new Stack<ThreadLocaVFL>();
                threadLoggerStack.push(subLogger);
                THREAD_VFL_STACK.set(threadLoggerStack);

                //Run the provided fn
                R r = this.fnHandler(callable, endMsgFn);
                if (!THREAD_VFL_STACK.get().isEmpty()) {
                    throw new IllegalStateException("Logger stack for new thread's block " + blockName + " is NOT empty");
                }
                return r;
            } finally {
                //once callable is done processing. Remove the logger stack. It should be of size 1.
                THREAD_VFL_STACK.remove();
            }
        }, executor);
    }

    @Override
    public <R> R call(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn) {
        ensureBlockStarted();
        String subBlockId = generateUID();
        var b = createBlockDataAndPush(subBlockId, blockName);
        var l = createLogAndPush(VflLogType.SUB_BLOCK_START, message, subBlockId);
        var s = new VFLBlockContext(b, blockContext.buffer);
        var subLogger = new ThreadLocaVFL(s);
        THREAD_VFL_STACK.get().push(subLogger);
        blockContext.currentLogId = l.getId();
        return this.fnHandler(callable, endMsgFn);
    }
}