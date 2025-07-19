package dev.kuku.vfl.core;

import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public class VFLogger implements VFL {
    protected final VFLBlockContext blockContext;

    public VFLogger(VFLBlockContext blockContext) {
        this.blockContext = blockContext;
    }

    protected LogData createLogAndPush(VflLogType logType, String message, String referencedBlockId) {
        var ld = new LogData(generateUID(),
                this.blockContext.blockInfo.getId(),
                this.blockContext.currentLogId,
                logType,
                message,
                referencedBlockId,
                Instant.now().toEpochMilli());
        this.blockContext.buffer.pushLogToBuffer(ld);
        return ld;
    }

    protected void ensureBlockStarted() {
        if (blockContext.blockStarted.compareAndSet(false, true)) {
            createLogAndPush(VflLogType.BLOCK_START, null, null);
        }
    }

    @Override
    public void msg(String message) {
        ensureBlockStarted();
        var ld = createLogAndPush(VflLogType.MESSAGE, message, null);
        this.blockContext.currentLogId = ld.getId();
    }

    private <R> R fn(Callable<R> fn, Function<R, String> messageFn, VflLogType logType) {
        R r;
        String msg = null;
        try {
            r = fn.call();
            msg = messageFn.apply(r);
        } catch (Exception e) {
            msg = "Failed to process message " + e.getClass().getSimpleName() + " - " + e.getMessage();
            throw new RuntimeException(e);
        } finally {
            blockContext.currentLogId = createLogAndPush(logType, msg, null).getId();
        }
        return r;
    }

    @Override
    public <R> R msgFn(Callable<R> fn, Function<R, String> messageFn) {
        ensureBlockStarted();
        return this.fn(fn, messageFn, VflLogType.MESSAGE);
    }

    @Override
    public void warn(String message) {
        ensureBlockStarted();
        this.blockContext.currentLogId = createLogAndPush(VflLogType.WARN, message, null).getId();
    }

    @Override
    public <R> R warnFn(Callable<R> fn, Function<R, String> messageFn) {
        ensureBlockStarted();
        return this.fn(fn, messageFn, VflLogType.WARN);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        this.blockContext.currentLogId = createLogAndPush(VflLogType.EXCEPTION, message, null).getId();
    }

    @Override
    public <R> R errorFn(Callable<R> fn, Function<R, String> messageFn) {
        ensureBlockStarted();
        return this.fn(fn, messageFn, VflLogType.EXCEPTION);
    }

    @Override
    public void closeBlock(String endMessage) {
        ensureBlockStarted();
        createLogAndPush(VflLogType.BLOCK_END, endMessage, null);
    }
}
