package dev.kuku.vfl.scopedLogger;

import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;
import static dev.kuku.vfl.scopedLogger.ScopedValueBlockContext.scopedBlockContext;

public class ScopedLoggerImpl implements ScopedLogger {
    private static ScopedLoggerImpl instance;

    private ScopedLoggerImpl() {
    }

    //Not possible to make it static since we implement interface, so we use singleton instead.
    public static ScopedLogger get() {
        if (!scopedBlockContext.isBound()) {
            throw new IllegalStateException("scopedBlockData is not within ScopedValue bound. Please use " + ScopedLoggerRunner.class.getName() + " to start a new scope.");
        }
        if (instance == null) {
            instance = new ScopedLoggerImpl();
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
    public void text(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.MESSAGE, null);
    }

    @Override
    public void textHere(String message) {
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
    public <R> R textFn(String message, Callable<R> fn) {
        return this.textFnHandler(fn, _ -> message, false);
    }

    @Override
    public <R> R textFnHere(String message, Callable<R> fn) {
        return this.textFnHandler(fn, _ -> message, true);
    }

    @Override
    public <R> R textFn(Callable<R> fn, Function<R, String> messageFn) {
        return this.textFnHandler(fn, messageFn, false);
    }

    @Override
    public <R> R textFnHere(Callable<R> fn, Function<R, String> messageFn) {
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
        ScopedBlockContext subBlockLoggerContext = new ScopedBlockContext(sbd, scopedBlockContext.get().buffer);
        return ScopedLoggerUtil.subBlockFnHandler(blockName, endMessageFn, callable, subBlockLoggerContext);
    }

    /*
     * New threads both virtual and platform thread needs to be run as async. <br>
     * Virtual threads get unmounted when blocking and mounted when blocking operation is complete.
     * They may get mounted in a different thread than the thread which contains the scoped value or even in a thread that contains a different value for the same scoped value
     */
    private <R> Future<R> asyncSubBlockFnHandler(String blockName, String message, Function<R, String> endMessageFn, Callable<R> callable, Executor executor, boolean stay) {
        ensureBlockStarted();
        var currentContext = scopedBlockContext.get();
        //Copy the current context to the new thread's scoped value which will then use it to create another nested scope when it calls subBlockFnHandler
        if (executor != null) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return ScopedValue.where(scopedBlockContext, currentContext)
                            .call(() -> this.subBlockFnHandler(blockName, message, endMessageFn, callable, stay));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return ScopedValue.where(scopedBlockContext, currentContext)
                        .call(() -> this.subBlockFnHandler(blockName, message, endMessageFn, callable, stay));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void run(String blockName, String message, Runnable runnable) {
        this.subBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, false);
    }

    @Override
    public Future<Void> runAsync(String blockName, String message, Runnable runnable) {
        return this.asyncSubBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, null, false);
    }

    @Override
    public Future<Void> runAsync(String blockName, String message, Runnable runnable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, executor, false);
    }

    @Override
    public void runHere(String blockName, String message, Runnable runnable) {
        this.subBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, true);
    }

    @Override
    public Future<Void> runHereAsync(String blockName, String message, Runnable runnable) {
        return this.asyncSubBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, null, true);
    }

    @Override
    public Future<Void> runHereAsync(String blockName, String message, Runnable runnable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, null, () -> {
            runnable.run();
            return null;
        }, executor, true);
    }

    @Override
    public <T> T call(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, false);
    }

    @Override
    public <T> Future<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.asyncSubBlockFnHandler(blockName, message, endMessageFn, callable, null, false);
    }

    @Override
    public <T> Future<T> callAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, endMessageFn, callable, executor, false);
    }

    @Override
    public <T> T call(String blockName, String message, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, false);
    }

    @Override
    public <T> Future<T> callAsync(String blockName, String message, Callable<T> callable) {
        return this.asyncSubBlockFnHandler(blockName, message, null, callable, null, false);
    }

    @Override
    public <T> Future<T> callAsync(String blockName, String message, Callable<T> callable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, null, callable, executor, false);
    }

    @Override
    public <T> T callHere(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, true);
    }

    @Override
    public <T> Future<T> callHereAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable) {
        return this.asyncSubBlockFnHandler(blockName, message, endMessageFn, callable, null, true);
    }

    @Override
    public <T> Future<T> callHereAsync(String blockName, String message, Function<T, String> endMessageFn, Callable<T> callable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, endMessageFn, callable, executor, true);
    }

    @Override
    public <T> T callHere(String blockName, String message, Callable<T> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, true);
    }

    @Override
    public <T> Future<T> callHereAsync(String blockName, String message, Callable<T> callable) {
        return this.asyncSubBlockFnHandler(blockName, message, null, callable, null, true);
    }

    @Override
    public <T> Future<T> callHereAsync(String blockName, String message, Callable<T> callable, Executor executor) {
        return this.asyncSubBlockFnHandler(blockName, message, null, callable, executor, true);
    }

    @Override
    public void closeBlock(String endMessage) {
        ensureBlockStarted();
        createAndPushLogData(endMessage, VflLogType.BLOCK_END, null);
    }
}