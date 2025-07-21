package dev.kuku.vfl.threadLocal;

import dev.kuku.vfl.StartBlockHelper;
import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LoggerAndBlockLogData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.StartBlockHelper.CreateBlockDataAndPush;
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
        buffer.pushBlockToBuffer(rootCtx.blockInfo);
        return SetupNewThreadLoggerStackAndCall(parentLogger, callable);
    }

    private static <R> R SetupNewThreadLoggerStackAndCall(ThreadLocaVFL logger, Callable<R> callable) {
        Stack<ThreadLocaVFL> loggerStack = new Stack<>();
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

    public static ThreadLocaVFL Get() {
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

    @Override
    public void closeBlock(String endMessage) {
        super.closeBlock(endMessage);
        //Pop the logger from the thread stack.
        var current = ThreadLocaVFL.Get();
        if (current != this) {
            throw new IllegalStateException("Current logger is not the latest logger in stack");
        }
        THREAD_VFL_STACK.get().pop();
    }

    private LoggerAndBlockLogData setupBlockStart(String blockName, String startMsg) {
        ensureBlockStarted();
        String subBlockId = generateUID();
        var b = CreateBlockDataAndPush(subBlockId, blockName, blockContext);
        var l = createLogAndPush(VflLogType.SUB_BLOCK_START, startMsg, subBlockId);
        var s = new VFLBlockContext(b, blockContext.buffer);
        var subLogger = new ThreadLocaVFL(s);
        return new LoggerAndBlockLogData(subLogger, b, l);
    }

    private static <R> R ProcessCallableInCurrentThreadLogger(Callable<R> callable, Function<R, String> endMsgFn) {
        //Get the latest pushed logger and pass it to block function handler
        ThreadLocaVFL subLogger = ThreadLocaVFL.Get();
        return StartBlockHelper.callFnForLogger(callable, endMsgFn, null, subLogger);
    }

    @Override
    public <R> R call(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMsgFn) {
        //Ensure that the block has started
        ensureBlockStarted();
        //Create and push log&block data to buffer, move forward if desired and returns them.
        StartBlockHelper.setupStartBlock(blockName, startMessage, true, blockContext, ThreadLocaVFL::new,
                loggerAndBlockLogData -> THREAD_VFL_STACK.get().push((ThreadLocaVFL) loggerAndBlockLogData.logger()));
        return ProcessCallableInCurrentThreadLogger(callable, endMsgFn);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor) {
        ensureBlockStarted();
        //Setup start block
        var startResult = StartBlockHelper.setupStartBlock(blockName, message, false, blockContext, ThreadLocaVFL::new, null);
        //return a completable future within which we setup thread logger stack and then use ProcessCallableInCurrentThreadLogger to execute the callable.
        return CompletableFuture.supplyAsync(() -> ThreadLocaVFL.SetupNewThreadLoggerStackAndCall(
                (ThreadLocaVFL) startResult.logger(), () -> ProcessCallableInCurrentThreadLogger(callable, endMsgFn)
        ), executor);
    }


}