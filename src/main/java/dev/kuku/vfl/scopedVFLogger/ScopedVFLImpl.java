package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.VFLogger;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public class ScopedVFLImpl extends VFLogger implements ScopedVFL {

    ScopedVFLImpl(VFLBlockContext context) {
        super(context);
    }

    /**
     * Returns the current scope's {@link ScopedVFL}
     * The method {@link Helper#subBlockFnHandler(String, Function, Callable, ScopedVFL)} Ensures that current scope's instance is always valid. <br> <br>
     *
     * @return current scope's {@link ScopedVFL}
     */
    public static ScopedVFL get() {
        if (!ScopedValueVFLContext.scopedInstance.isBound()) {
            throw new IllegalStateException("scopedBlockData is not within ScopedValue bound. Please use " + Runner.class.getName() + " to start a new scope.");
        }
        return ScopedValueVFLContext.scopedInstance.get();
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        var bd = new BlockData(id, blockContext.blockInfo.getId(), blockName);
        blockContext.buffer.pushBlockToBuffer(bd);
        return bd;
    }

    private <R> R runHandler(String blockName, String blockMessage, Callable<R> callable, boolean move) {
        ensureBlockStarted();
        String subBlockId = generateUID();
        BlockData subBlockContext = createAndPushBlockData(subBlockId, blockName);
        LogData subBlockStartLog = createLogAndPush(VflLogType.SUB_BLOCK_START, blockMessage, subBlockId);
        VFLBlockContext subBlockLoggerContext = new VFLBlockContext(subBlockContext, super.blockContext.buffer);
        //Will set this as the nested scope's ScopedVFL value.
        ScopedVFL subBlockLogger = new ScopedVFLImpl(subBlockLoggerContext);
        if (move) {
            super.blockContext.currentLogId = subBlockStartLog.getId();
        }
        return Helper.subBlockFnHandler(blockName, null, callable, subBlockLogger);
    }

    @Override
    public void run(String blockName, String blockMessage, Runnable runnable) {
        runHandler(blockName, blockMessage, () -> {
            runnable.run();
            return null;
        }, true);
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable, Executor executor) {
        return null;
    }

    @Override
    public <R> R call(String blockName, String blockMessage, Callable<R> callable) {
        return runHandler(blockName, blockMessage, callable, true);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Callable<R> callable, Executor executor) {
        return null;
    }
}