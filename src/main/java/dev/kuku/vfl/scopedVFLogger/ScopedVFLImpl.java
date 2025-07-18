package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;
import static dev.kuku.vfl.scopedVFLogger.ScopedValueVFLContext.scopedBlockContext;

public class ScopedVFLImpl implements ScopedVFL {
    private static ScopedVFLImpl instance;

    private ScopedVFLImpl() {
    }

    //Not possible to make it static since we implement interface, so we use singleton instead.
    public static ScopedVFL get() {
        if (!scopedBlockContext.isBound()) {
            throw new IllegalStateException("scopedBlockData is not within ScopedValue bound. Please use " + ScopedVFLRunner.class.getName() + " to start a new scope.");
        }
        if (instance == null) {
            instance = new ScopedVFLImpl();
        }
        return instance;
    }

    private void ensureBlockStarted() {
        if (scopedBlockContext.get().blockStarted.compareAndSet(false, true)) {
            createAndPushLogData(null, VflLogType.BLOCK_START, null);
        }
    }

    private LogData createAndPushLogData(String message, VflLogType logType, String referencedBlockId) {
        var ld = new LogData(generateUID(),
                scopedBlockContext.get().blockInfo.getId(),
                scopedBlockContext.get().currentLog == null ? null : scopedBlockContext.get().currentLog.getId(),
                logType,
                message,
                referencedBlockId,
                Instant.now().toEpochMilli());
        scopedBlockContext.get().buffer.pushLogToBuffer(ld);
        return ld;
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        var bd = new BlockData(id, scopedBlockContext.get().blockInfo.getId(), blockName);
        scopedBlockContext.get().buffer.pushBlockToBuffer(bd);
        return bd;
    }

    @Override
    public void msg(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.MESSAGE, null);
    }

    @Override
    public void msgHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.MESSAGE, null);
    }

    private <R> R textFnHandler(Callable<R> callable, Function<R, String> messageFn, boolean stay) {
        try {
            R result = callable.call();
            String message;
            try {
                message = messageFn.apply(result);
            } catch (Exception e) {
                message = String.format("Failed to invoke function  for textFn %s", messageFn.apply(result));
            }
            var textLog = createAndPushLogData(message, VflLogType.MESSAGE, null);
            if (!stay) {
                scopedBlockContext.get().currentLog = textLog;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> R msgFn(String message, Callable<R> fn) {
        return this.textFnHandler(fn, _ -> message, false);
    }

    @Override
    public <R> R msgFnHere(String message, Callable<R> fn) {
        return this.textFnHandler(fn, _ -> message, true);
    }

    @Override
    public <R> R msgFn(Callable<R> fn, Function<R, String> messageFn) {
        return this.textFnHandler(fn, messageFn, false);
    }

    @Override
    public <R> R msgFnHere(Callable<R> fn, Function<R, String> messageFn) {
        return this.textFnHandler(fn, messageFn, true);
    }

    @Override
    public void warn(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void warnHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.EXCEPTION, null);
    }

    @Override
    public void errorHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.EXCEPTION, null);
    }

    private <R> R subBlockFnHandler(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable, boolean stay) {
        ensureBlockStarted();
        //Create subblock and push it
        String subBlockId = generateUID();
        var sbd = createAndPushBlockData(subBlockId, blockName);
        //Create subblock start log and push it
        var subBLockStartLog = createAndPushLogData(message, VflLogType.SUB_BLOCK_START, subBlockId);
        //Stay or move
        if (!stay) {
            scopedBlockContext.get().currentLog = subBLockStartLog;
        }
        //Create the subblock logger data for subblock
        ScopedVFLContext subBlockLoggerContext = new ScopedVFLContext(sbd, scopedBlockContext.get().buffer);
        return Helper.subBlockFnHandler(blockName, endMessageFn, callable, subBlockLoggerContext);
    }

    /*
     * New threads both virtual and platform thread needs to be run as async. <br>
     * Virtual threads get unmounted when blocking and mounted when blocking operation is complete.
     * They may get mounted in a different thread than the thread which contains the scoped value or even in a thread that contains a different value for the same scoped value
     */
    private <R> CompletableFuture<R> asyncSubBlockFnHandler(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable, Executor executor) {
        ensureBlockStarted();
        //Create subblock and push it
        String subBlockId = generateUID();
        var sbd = createAndPushBlockData(subBlockId, blockName);
        //Create subblock start log and push it
        var subBLockStartLog = createAndPushLogData(message, VflLogType.SUB_BLOCK_START, subBlockId);
        //Create the subblock logger data for subblock
        ScopedVFLContext subBlockLoggerContext = new ScopedVFLContext(sbd, scopedBlockContext.get().buffer);
        if (executor != null) {
            //Starting a new scope IN the executor's thread so it will have access to sub logger context
            return CompletableFuture.supplyAsync(() -> Helper.subBlockFnHandler(blockName, endMessageFn, callable, subBlockLoggerContext), executor);
        } else {
            //Starting a new scope IN the forked thread so it will have access to sub logger context
            return CompletableFuture.supplyAsync(() -> Helper.subBlockFnHandler(blockName, endMessageFn, callable, subBlockLoggerContext));
        }
    }

    @Override
    public void run(String blockName, String message, Runnable runnable) {
        this.subBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, false);
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String message, Runnable runnable) {
        return this.asyncSubBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, null);
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String message, Runnable runnable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, executor);
    }

    @Override
    public void runHere(String blockName, String message, Runnable runnable) {
        this.subBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, true);
    }

    @Override
    public <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, false);
    }

    @Override
    public <T> CompletableFuture<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.asyncSubBlockFnHandler(blockName, message, endMessageFn, callable, null);
    }

    @Override
    public <T> CompletableFuture<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, endMessageFn, callable, executor);
    }

    @Override
    public <T> T call(String blockName, String message, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, false);
    }

    @Override
    public <T> CompletableFuture<T> callAsync(String blockName, String message, Callable<T> callable) {
        return this.asyncSubBlockFnHandler(blockName, message, null, callable, null);
    }

    @Override
    public <T> CompletableFuture<T> callAsync(String blockName, String message, Callable<T> callable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, null, callable, executor);
    }

    @Override
    public <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, true);
    }

    @Override
    public <T> T callHere(String blockName, String message, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, true);
    }

    @Override
    public void closeBlock(String endMessage) {
        ensureBlockStarted();
        createAndPushLogData(endMessage, VflLogType.BLOCK_END, null);
    }
}