package dev.kuku.vfl.scoped;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.models.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public class ScopedVFL extends VFL implements IScopedVFL {

    /// Holds instance per scope that attempts to access it.
    public static final ScopedValue<IScopedVFL> scopedInstance =
            ScopedValue.newInstance();

    ScopedVFL(VFLBlockContext context) {
        super(context);
    }

    /**
     * Returns the current scope's {@link IScopedVFL}
     * The method {@link Helper#blockFnLifeCycleHandler(String, Function, Callable, IScopedVFL)} Ensures that current scope's instance is always valid. <br> <br>
     *
     * @return current scope's {@link IScopedVFL}
     */
    public static IScopedVFL get() {
        if (!ScopedVFL.scopedInstance.isBound()) {
            throw new IllegalStateException(
                    "scopedBlockData is not within ScopedValue bound. Please use " +
                            IScopedVFL.Runner.class.getName() +
                            " to start a new scope."
            );
        }
        return ScopedVFL.scopedInstance.get();
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        var bd = new BlockData(id, blockContext.blockInfo.getId(), blockName);
        blockContext.buffer.pushBlockToBuffer(bd);
        return bd;
    }

    private LoggerAndBlockLogData setupBlockStart(String blockName, String blockMessage) {
        String subBlockId = generateUID();
        BlockData subBlockData = createAndPushBlockData(subBlockId, blockName);
        LogData subBlockStartLog = createLogAndPush(
                VflLogType.SUB_BLOCK_START,
                blockMessage,
                subBlockId
        );
        VFLBlockContext subBlockLoggerCtx = new VFLBlockContext(subBlockData, blockContext.buffer);
        var logger = new ScopedVFL(subBlockLoggerCtx);
        return new LoggerAndBlockLogData(logger, subBlockData, subBlockStartLog);
    }

    private <R> R fnHandler(
            String blockName,
            String blockMessage,
            Function<R, String> endMessageFn,
            Callable<R> callable,
            boolean move
    ) {
        LoggerAndBlockLogData initResult = setupBlockStart(blockName, blockMessage);
        IScopedVFL subBlockLogger = (IScopedVFL) initResult.logger();
        if (move) {
            super.blockContext.currentLogId = initResult.blockData().getId();
        }
        return Helper.blockFnLifeCycleHandler(
                blockName,
                endMessageFn,
                callable,
                subBlockLogger
        );
    }

    private <R> CompletableFuture<R> asyncFnHandler(
            String blockName,
            String message,
            Function<R, String> endMessageFn,
            Callable<R> callable,
            Executor executor
    ) {
        //Create a copy of the current context so that we can pass it to the executing thread
        var currentLogger = ScopedVFL.scopedInstance.get();
        Supplier<R> l = () -> {
            //Create a root scope in the executing thread with currentLog provided as the scope's instance.
            return ScopedValue.where(ScopedVFL.scopedInstance, currentLogger)
                    //fnHandler will attempt to access scopedInstance and will get currentLogger instance
                    // without this it will throw unbounded exception as the executing thread will not have any scopedInstance since scopedValue do not propagate the value across threads.
                    .call(() ->
                            fnHandler(blockName, message, endMessageFn, callable, false)
                    );
        };
        if (executor != null) {
            return CompletableFuture.supplyAsync(l, executor);
        }
        return CompletableFuture.supplyAsync(l);
    }

    @Override
    public void run(String blockName, String blockMessage, Runnable runnable) {
        ensureBlockStarted();
        fnHandler(
                blockName,
                blockMessage,
                null,
                () -> {
                    runnable.run();
                    return null;
                },
                true
        );
    }

    @Override
    public CompletableFuture<Void> runAsync(
            String blockName,
            String blockMessage,
            Runnable runnable,
            Executor executor
    ) {
        ensureBlockStarted();
        return asyncFnHandler(
                blockName,
                blockMessage,
                null,
                () -> {
                    runnable.run();
                    return null;
                },
                executor
        );
    }

    @Override
    public <R> R call(
            String blockName,
            String blockMessage,
            Function<R, String> endMessageFn,
            Callable<R> callable
    ) {
        ensureBlockStarted();
        return fnHandler(blockName, blockMessage, endMessageFn, callable, true);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(
            String blockName,
            String blockMessage,
            Function<R, String> endMessageFn,
            Callable<R> callable,
            Executor executor
    ) {
        ensureBlockStarted();
        return asyncFnHandler(
                blockName,
                blockMessage,
                endMessageFn,
                callable,
                executor
        );
    }
}
