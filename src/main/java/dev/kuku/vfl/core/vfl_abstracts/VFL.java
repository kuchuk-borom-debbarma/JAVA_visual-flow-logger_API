package dev.kuku.vfl.core.vfl_abstracts;

import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class VFL {
    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);

    // Called to ensure that the logging block is started.
    public final void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            getContext().buffer.pushBlockToBuffer(getContext().blockInfo);
        }
    }

    // Closes the log block.
    public void close(String endMessage) {
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
        // Check if logging for this type is allowed.
        if (!getContext().allowedLogTypes.contains(type.toString())) {
            return;
        }

        // Ensure the log block is started.
        ensureBlockStarted();

        // Create and push the new log entry using the provided type.
        var createdLog = VFLHelper.CreateLogAndPush2Buffer(
                getContext().blockInfo.getId(),
                getContext().currentLogId,
                type,           // LogTypeEnum value (MESSAGE, WARN, or ERROR)
                mesage,
                getContext().buffer);

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

}