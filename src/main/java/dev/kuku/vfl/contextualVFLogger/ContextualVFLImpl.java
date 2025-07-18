package dev.kuku.vfl.contextualVFLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;

class ContextualVFLImpl implements ContextualVFL {
    private final AtomicBoolean blockStarted = new AtomicBoolean(false);
    private final BlockData blockInfo;
    private final VFLBuffer buffer;
    private LogData currentLogData;

    ContextualVFLImpl(BlockData blockInfo, VFLBuffer buffer) {
        this.blockInfo = blockInfo;
        this.buffer = buffer;
    }

    /// Create log, push it to buffer and return it.
    private LogData createLogAndPush(VflLogType logType, String message, String referencedBlockId) {
        LogData ld = new LogData(generateUID(), this.blockInfo.getId(),
                this.currentLogData == null ? null : this.currentLogData.getId(), logType, message, referencedBlockId, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(ld);
        return ld;
    }

    ///  Create block data, push it to buffer and return it.
    private BlockData createBlockAndPush(String id, String blockName) {
        BlockData bd = new BlockData(id, this.blockInfo.getId(), blockName);
        buffer.pushBlockToBuffer(bd);
        return bd;
    }

    /// Creates a new log of type block_start if it has not been created before. Uses atomic boolean to get and set in one instruction
    private void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            createLogAndPush(VflLogType.BLOCK_START, null, null);
        }
    }

    ///  creates sub_block_start log, creates sub_block_log, and it's logger and then executes the passed callable.<br>
    /// Catches exception and adds an error log before re-throwing. <br>
    /// Closes the subblock logger once callable has finished executing.
    private <R> R subBlockFnHandler(String blockName, String message, Function<R, String> endMessageFn, Function<ContextualVFL, R> callable, boolean stay) {
        String subBlockId = generateUID();
        BlockData bd = createBlockAndPush(subBlockId, blockName);
        LogData ld = createLogAndPush(VflLogType.SUB_BLOCK_START, message, subBlockId);
        if (!stay) {
            currentLogData = ld;
        }
        ContextualVFL subBlockLogger = new ContextualVFLImpl(bd, buffer);
        return Helper.blockFnHandler(blockName, message, endMessageFn, callable, subBlockLogger);
    }

    @Override
    public void run(String blockName, String message, Consumer<ContextualVFL> runnable) {
        ensureBlockStarted();
        this.subBlockFnHandler(blockName, message, null, (logger) -> {
            runnable.accept(logger);
            return null;
        }, false);
    }

    @Override
    public void runHere(String blockName, String message, Consumer<ContextualVFL> runnable) {
        this.subBlockFnHandler(blockName, message, null, (logger) -> {
            runnable.accept(logger);
            return null;
        }, true);
    }

    @Override
    public <R> R call(String blockName, String message, Function<R, String> endMessageFn, Function<ContextualVFL, R> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, false);
    }

    @Override
    public <R> R call(String blockName, String message, Function<ContextualVFL, R> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, false);
    }

    @Override
    public <R> R callHere(String blockName, String message, Function<R, String> endMessageFn, Function<ContextualVFL, R> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, true);
    }

    @Override
    public <R> R callHere(String blockName, String message, Function<ContextualVFL, R> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, true);
    }

    @Override
    public void text(String message) {
        ensureBlockStarted();
        currentLogData = createLogAndPush(VflLogType.MESSAGE, message, null);
    }

    @Override
    public void textHere(String message) {
        ensureBlockStarted();
        createLogAndPush(VflLogType.MESSAGE, message, null);
    }


    private <R> R textFnHandler(Callable<R> fn, Function<R, String> messageFn, boolean stay) {
        ensureBlockStarted();
        try {
            var result = fn.call();
            String message;
            try {
                message = messageFn.apply(result);
            } catch (Exception e) {
                message = "Failed to process message : " + e.getClass().getSimpleName() + " : " + e.getMessage();
            }
            var msgLog = createLogAndPush(VflLogType.MESSAGE, message, null);
            if (!stay) {
                currentLogData = msgLog;
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> R textFn(String message, Callable<R> fn) {
        return this.textFnHandler(fn, (_) -> message, false);
    }

    @Override
    public <R> R textFnHere(String message, Callable<R> fn) {
        return this.textFnHandler(fn, (_) -> message, true);
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
        currentLogData = createLogAndPush(VflLogType.WARN, message, null);
    }

    @Override
    public void warnHere(String message) {
        ensureBlockStarted();
        createLogAndPush(VflLogType.WARN, message, null);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        currentLogData = createLogAndPush(VflLogType.EXCEPTION, message, null);
    }

    @Override
    public void errorHere(String message) {
        ensureBlockStarted();
        createLogAndPush(VflLogType.EXCEPTION, message, null);
    }

    @Override
    public void closeBlock(String endMessage) {
        ensureBlockStarted();
        createLogAndPush(VflLogType.BLOCK_END, endMessage, null);
    }
}
