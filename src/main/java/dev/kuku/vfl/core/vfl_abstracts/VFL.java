package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class VFL {
    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);

    // Called to ensure that the logging block is started.
    public final void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            getContext().buffer.pushLogStartToBuffer(getContext().blockInfo.getId(), Instant.now().toEpochMilli());
        }
    }

    // Closes the log block.
    public void close(String endMessage) {
        ensureBlockStarted();
        getContext().buffer.pushLogEndToBuffer(getContext().blockInfo.getId(), new BlockEndData(Instant.now().toEpochMilli(), endMessage));
    }

    /**
     * Internal logging method that encapsulates the common logic.
     *
     * @param type    The log type (e.g. MESSAGE, WARN, or ERROR).
     * @param message The message to log.
     */
    private void logInternal(LogTypeEnum type, String message) {
        // Ensure the log block is started.
        ensureBlockStarted();

        // Create and push the new log entry using the provided type.
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getContext().blockInfo.getId(), getContext().currentLogId, type, message, getContext().buffer);

        // Update the current log id to "move" forward the flow.
        getContext().currentLogId = createdLog.getId();
    }

    private <R> R logFnInternal(LogTypeEnum type, Supplier<R> fn, Function<R, String> messageSerializer) {
        var r = fn.get();
        String msg = messageSerializer.apply(r);
        logInternal(type, msg);
        return r;
    }

    // Public logging methods that simply forward to the internal method.
    public final void log(String message) {
        logInternal(LogTypeEnum.MESSAGE, message);
    }

    public final <R> R logFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.MESSAGE, fn, messageSerializer);
    }

    public final void warn(String message) {
        logInternal(LogTypeEnum.WARN, message);
    }

    public final <R> R warnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.WARN, fn, messageSerializer);
    }

    public final void error(String message) {
        logInternal(LogTypeEnum.ERROR, message);
    }

    public final <R> R errorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.ERROR, fn, messageSerializer);
    }

    // Abstract method that subclasses must implement to provide context.
    abstract protected VFLBlockContext getContext();
}