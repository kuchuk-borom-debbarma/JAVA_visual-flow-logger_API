package dev.kuku.vfl;

import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

class VFL implements IVFL {
    protected final VFLBlockContext blockContext;

    protected VFL(VFLBlockContext blockContext) {
        this.blockContext = blockContext;
    }

    protected Log createLogAndPush(VflLogType logType, String message, String referencedBlockId, boolean isSecondary) {
        var ld = new Log(generateUID(),
                this.blockContext.blockInfo.getId(),
                this.blockContext.currentLogId,
                logType,
                message,
                referencedBlockId,
                Instant.now().toEpochMilli(), isSecondary);
        this.blockContext.buffer.pushLogToBuffer(ld);
        return ld;
    }

    protected void ensureBlockStarted() {
        if (blockContext.blockStarted.compareAndSet(false, true)) {
            createLogAndPush(VflLogType.BLOCK_START, null, null, false);
        }
    }

    @Override
    public void msg(String message) {
        ensureBlockStarted();
        var ld = createLogAndPush(VflLogType.MESSAGE, message, null, false);
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
            //TODO: option to silently skip exception or keep it. For fluent too
            throw new RuntimeException(e);
        } finally {
            blockContext.currentLogId = createLogAndPush(logType, msg, null, false).getId();
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
        this.blockContext.currentLogId = createLogAndPush(VflLogType.WARN, message, null, false).getId();
    }

    @Override
    public <R> R warnFn(Callable<R> fn, Function<R, String> messageFn) {
        ensureBlockStarted();
        return this.fn(fn, messageFn, VflLogType.WARN);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        this.blockContext.currentLogId = createLogAndPush(VflLogType.EXCEPTION, message, null, false).getId();
    }

    @Override
    public <R> R errorFn(Callable<R> fn, Function<R, String> messageFn) {
        ensureBlockStarted();
        return this.fn(fn, messageFn, VflLogType.EXCEPTION);
    }

    @Override
    public void closeBlock(String endMessage) {
        ensureBlockStarted();
        createLogAndPush(VflLogType.BLOCK_END, endMessage, null, false);
    }
}
