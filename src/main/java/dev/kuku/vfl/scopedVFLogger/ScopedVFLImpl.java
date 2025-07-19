package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.VFLogger;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public class ScopedVFLImpl extends VFLogger implements ScopedVFL {

    private ScopedVFLImpl(VFLBlockContext context) {
        super(context);
    }

    //Not possible to make it static since we implement interface, so we use singleton instead.
    public static ScopedVFL get() {
        if (!ScopedValueVFLContext.scopedBlockContextAndInstance.isBound()) {
            throw new IllegalStateException("scopedBlockData is not within ScopedValue bound. Please use " + ScopedVFLRunner.class.getName() + " to start a new scope.");
        }
        return ScopedValueVFLContext.scopedBlockContextAndInstance.get().scopedInstance;
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
        ScopedVFLContextAndInstance subBlockContextAndInstance = new ScopedVFLContextAndInstance(subBlockLoggerContext, new ScopedVFLImpl(subBlockLoggerContext));
        if (move) {
            super.blockContext.currentLogId = subBlockStartLog.getId();
        }
        return Helper.subBlockFnHandler(blockName, null, callable, subBlockContextAndInstance);
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

    public static class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> fn) {
            var rootBlockContext = new BlockData(generateUID(), null, blockName);
            buffer.pushBlockToBuffer(rootBlockContext);
            var vflContext = new VFLBlockContext(rootBlockContext, buffer);
            ScopedVFLContextAndInstance contextAndInstance = new ScopedVFLContextAndInstance(vflContext, new ScopedVFLImpl(vflContext));
            try {
                return Helper.subBlockFnHandler(blockName, null, fn, contextAndInstance);
            } finally {
                buffer.flushAndClose();
            }
        }

        public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
            Runner.call(blockName, buffer, () -> {
                runnable.run();
                return null;
            });
        }
    }
}