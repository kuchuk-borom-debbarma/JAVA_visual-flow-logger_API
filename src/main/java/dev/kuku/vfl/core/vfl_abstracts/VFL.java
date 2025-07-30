package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;
import static dev.kuku.vfl.core.helpers.Util.UID;

public abstract class VFL {
    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);

    // Called to ensure that the logging block is started.
    public final void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            getContext().buffer.pushLogStartToBuffer(getContext().blockInfo.getId(), Instant.now().toEpochMilli());
        }
    }

    // Closes the log block.
    protected void close(String endMessage) {
        ensureBlockStarted();
        getContext().buffer.pushLogEndToBuffer(getContext().blockInfo.getId(), endMessage);
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
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getContext().blockInfo.getId(), getContext().currentLogId, type,           // LogTypeEnum value (MESSAGE, WARN, or ERROR)
                message, getContext().buffer);

        // Update the current log id.
        getContext().currentLogId = createdLog.getId();
    }

    private <R> R logFnInternal(LogTypeEnum type, Supplier<R> fn, Function<R, String> messageSerializer, Object... args) {
        var r = fn.get();
        String msg = messageSerializer.apply(r);
        logInternal(type, FormatMessage(msg, r, args));
        return r;
    }

    // Public logging methods that simply forward to the internal method.
    public final void log(String message) {
        logInternal(LogTypeEnum.MESSAGE, message);
    }

    public final <R> R logFn(Supplier<R> fn, Function<R, String> messageSerializer, Object... args) {
        return logFnInternal(LogTypeEnum.MESSAGE, fn, messageSerializer, args);
    }

    public final void warn(String message) {
        logInternal(LogTypeEnum.WARN, message);
    }

    public final <R> R warnFn(Supplier<R> fn, Function<R, String> messageSerializer, Object... args) {
        return logFnInternal(LogTypeEnum.WARN, fn, messageSerializer, args);
    }

    public final void error(String message) {
        logInternal(LogTypeEnum.ERROR, message);
    }

    public final <R> R errorFn(Supplier<R> fn, Function<R, String> messageSerializer, Object... args) {
        return logFnInternal(LogTypeEnum.ERROR, fn, messageSerializer, args);
    }

    // Abstract method that subclasses must implement to provide context.
    abstract protected VFLBlockContext getContext();


    public static class VFLHelper {

        public static Log CreateLogAndPush2Buffer(String blockId, String parentLogId, LogTypeEnum logType, String message, VFLBuffer buffer) {
            Log l = new Log(UID(), blockId, parentLogId, logType, message, Instant.now().toEpochMilli());
            buffer.pushLogToBuffer(l);
            return l;
        }

        public static SubBlockStartLog CreateLogAndPush2Buffer(String blockId, String parentLogId, String startMessage, String referencedBlockId, LogTypeBlockStartEnum logType, VFLBuffer buffer) {
            SubBlockStartLog l = new SubBlockStartLog(UID(), blockId, parentLogId, startMessage, referencedBlockId, logType);
            buffer.pushLogToBuffer(l);
            return l;
        }

        public static Block CreateBlockAndPush2Buffer(String blockName, String parentBlockId, VFLBuffer buffer) {
            Block b = new Block(UID(), parentBlockId, blockName);
            buffer.pushBlockToBuffer(b);
            return b;
        }

        public static <R> R CallFnWithLogger(Supplier<R> callable, VFL logger, Function<R, String> endMessageSerializer, Object... args) {
            R result = null;
            try {
                result = callable.get();
            } catch (Exception e) {
                logger.error("Exception occurred: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                throw e;
            } finally {
                String endMsg = null;
                if (endMessageSerializer != null) {
                    try {
                        endMsg = endMessageSerializer.apply(result);
                        endMsg = FormatMessage(endMsg, result, args);
                    } catch (Exception e) {
                        endMsg = "Failed to serialize end message: " + e.getMessage();
                    }
                }
                logger.close(endMsg);
            }
            return result;
        }
    }
}