package dev.kuku.vfl.executionLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;

class ExecutionLoggerImpl implements ExecutionLogger {
    private final BlockData blockInfo;
    private final VFLBuffer buffer;
    private final AtomicBoolean blockStarted = new AtomicBoolean(false);
    private volatile String currentLogId;

    public ExecutionLoggerImpl(BlockData blockInfo, VFLBuffer buffer) {
        this.blockInfo = blockInfo;
        this.buffer = buffer;
    }

    private LogData createAndPushLogData(VflLogType logType, String message, String referencedBlockId) {
        LogData ld = new LogData(generateUID(),
                blockInfo.getId(),
                currentLogId,
                logType,
                message,
                referencedBlockId,
                Instant.now().toEpochMilli());
        this.buffer.pushLogToBuffer(ld);
        return ld;
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        var bd = new BlockData(id, this.blockInfo.getId(), blockName);
        this.buffer.pushBlockToBuffer(bd);
        return bd;
    }

    private void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            createAndPushLogData(VflLogType.BLOCK_START, null, null);
        }
    }

    @Override
    public void text(String message) {
        ensureBlockStarted();
        currentLogId = createAndPushLogData(VflLogType.MESSAGE, message, null).getId();
    }

    @Override
    public void textHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(VflLogType.MESSAGE, message, null);
    }

    @Override
    public void warn(String message) {
        ensureBlockStarted();
        currentLogId = createAndPushLogData(VflLogType.WARN, message, null).getId();
    }

    @Override
    public void warnHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(VflLogType.WARN, message, null);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        currentLogId = createAndPushLogData(VflLogType.EXCEPTION, message, null).getId();
    }

    @Override
    public void errorHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(VflLogType.EXCEPTION, message, null);
    }

    private <R> R subBlockFnHandler(String blockName, String msg, Function<R, String> endMsgFn, Function<ExecutionLogger, R> fn, boolean stay) {
        String subBlockId = generateUID();
        var subBlockStartLog = createAndPushLogData(VflLogType.SUB_BLOCK_START, msg, subBlockId);
        var subBlockInfo = createAndPushBlockData(subBlockId, blockName);
        if (!stay) {
            currentLogId = subBlockStartLog.getId();
        }
        var subBlockLogger = new ExecutionLoggerImpl(subBlockInfo, this.buffer);
        R result = null;
        try {
            result = fn.apply(subBlockLogger);
        } catch (Exception e) {
            subBlockLogger.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
            throw new RuntimeException(e);
        } finally {
            String endMessage = null;
            if (endMsgFn != null) {
                try {
                    endMessage = endMsgFn.apply(result);
                } catch (Exception e) {
                    endMessage = "Error processing End Message : " + String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage());
                }
            }
            subBlockLogger.closeBlock(endMessage);
        }
        return result;
    }

    @Override
    public void run(String blockName, String message, Consumer<ExecutionLogger> runnable) {
        this.subBlockFnHandler(blockName, message, null, (logger) -> {
            runnable.accept(logger);
            return null;
        }, false);
    }

    @Override
    public void runHere(String blockName, String message, Consumer<ExecutionLogger> runnable) {
        this.subBlockFnHandler(blockName, message, null, (logger) -> {
            runnable.accept(logger);
            return null;
        }, true);
    }

    @Override
    public <R> R call(String blockName, String message, Function<R, String> endMessageFn, Function<ExecutionLogger, R> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, false);
    }

    @Override
    public <R> R call(String blockName, String message, Function<ExecutionLogger, R> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, false);
    }

    @Override
    public <R> R callHere(String blockName, String message, Function<R, String> endMessageFn, Function<ExecutionLogger, R> callable) {
        return this.subBlockFnHandler(blockName, message, endMessageFn, callable, true);
    }

    @Override
    public <R> R callHere(String blockName, String message, Function<ExecutionLogger, R> callable) {
        return this.subBlockFnHandler(blockName, message, null, callable, true);
    }

    @Override
    public void closeBlock(String endMessage) {
        createAndPushLogData(VflLogType.BLOCK_END, endMessage, null);
    }
}
