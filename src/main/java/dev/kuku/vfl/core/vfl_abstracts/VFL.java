package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.Log;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static dev.kuku.vfl.core.helpers.Util.UID;

public abstract class VFL {
    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);

    // Called to ensure that the logging block is started.
    public final void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            getContext().buffer.pushLogStartToBuffer(getContext().blockInfo.getId());
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
     * @param type   The log type (e.g. MESSAGE, WARN, or ERROR).
     * @param mesage The message to log.
     */
    private void logInternal(LogTypeEnum type, String mesage) {
        // Ensure the log block is started.
        ensureBlockStarted();

        // Create and push the new log entry using the provided type.
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(getContext().blockInfo.getId(), getContext().currentLogId, type,           // LogTypeEnum value (MESSAGE, WARN, or ERROR)
                mesage, getContext().buffer);

        // Update the current log id.
        getContext().currentLogId = createdLog.getId();
    }

    // Public logging methods that simply forward to the internal method.
    public final void log(String mesage) {
        logInternal(LogTypeEnum.MESSAGE, mesage);
    }

    public final void warn(String mesage) {
        logInternal(LogTypeEnum.WARN, mesage);
    }

    public final void error(String mesage) {
        logInternal(LogTypeEnum.ERROR, mesage);
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

        public static <R> R CallFnWithLogger(Callable<R> callable, VFL logger, Function<R, String> endMessageSerializer) {
            R result = null;
            try {
                result = callable.call();
            } catch (RuntimeException e) {
                // Log the exception but preserve the original runtime exception
                logger.error("Exception occurred: " + e.getMessage());
                throw e; // Re-throw the original RuntimeException
            } catch (Exception e) {
                // For checked exceptions, wrap but preserve the cause
                logger.error("Exception occurred: " + e.getMessage());
                throw new RuntimeException("Exception in callable execution", e);
            } finally {
                String endMsg = null;
                if (endMessageSerializer != null) {
                    try {
                        endMsg = endMessageSerializer.apply(result);
                    } catch (Exception e) {
                        endMsg = "Failed to serialize end message: " + e.getMessage();
                    }
                }
                logger.close(endMsg);
            }
            return result;
        }

        public static Set<String> GetLogsAsStringSet(Set<LogTypeEnum> typeToRemove, Set<LogTypeBlockStartEnum> startLogTypeToRemove) {
            Set<String> set = new HashSet<>(LogTypeEnum.values().length + LogTypeBlockStartEnum.values().length);
            set.addAll(Arrays.stream(LogTypeEnum.values()).map(Object::toString).toList());
            set.addAll(Arrays.stream(LogTypeBlockStartEnum.values()).map(Object::toString).toList());
            if (typeToRemove != null && !typeToRemove.isEmpty()) {
                typeToRemove.stream().map(Object::toString).toList().forEach(set::remove);
            }
            if (startLogTypeToRemove != null && !startLogTypeToRemove.isEmpty()) {
                startLogTypeToRemove.stream().map(Object::toString).toList().forEach(set::remove);
            }
            return set;
        }
    }
}