package dev.kuku.vfl.core;

import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public abstract class VFL {

    protected final AtomicBoolean blockStarted = new AtomicBoolean(false);

    public final void ensureBlockStarted() {
        // Use compare-and-set for atomic, thread-safe initialization
        if (blockStarted.compareAndSet(false, true)) {
            final VFLBlockContext context = getContext();
            final long startTimestamp = Instant.now().toEpochMilli();
            context.buffer.pushLogStartToBuffer(context.blockInfo.getId(), startTimestamp);
        }
    }

    public void close(String endMessage) {
        ensureBlockStarted();

        final VFLBlockContext context = getContext();
        final long endTimestamp = Instant.now().toEpochMilli();
        final BlockEndData endData = new BlockEndData(endTimestamp, endMessage);

        context.buffer.pushLogEndToBuffer(context.blockInfo.getId(), endData);
    }

    private void logInternal(LogTypeEnum type, String message) {
        // Ensure the log block is started before writing any entries
        ensureBlockStarted();

        final VFLBlockContext context = getContext();

        // Create and push the new log entry using the specified type and current sequence
        final var createdLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                context.blockInfo.getId(),
                context.currentLogId,
                type,
                message,
                context.buffer
        );

        // Update the current log id to maintain proper sequencing for subsequent logs
        context.currentLogId = createdLog.getId();
    }

    private <R> R logFnInternal(LogTypeEnum type, Supplier<R> fn, Function<R, String> messageSerializer) {
        // Execute the function and capture its result
        final R result = fn.get();

        // Convert the result to a log message using the provided serializer
        final String message = messageSerializer.apply(result);

        // Log the message with the specified type
        logInternal(type, message);

        // Return the original result, allowing for method chaining
        return result;
    }

    // ========== PUBLIC LOGGING METHODS ==========
    // These methods provide the public API for different logging scenarios

    public final void info(String message) {
        logInternal(LogTypeEnum.MESSAGE, message);
    }

    /**
     * Executes a function and logs its result at the MESSAGE level.
     * This is useful for logging the outcome of operations without disrupting the flow.
     *
     * @param <R>               The return type of the function
     * @param fn                The function to execute
     * @param messageSerializer Function to convert the result to a log message
     * @return The result of the executed function
     */
    public final <R> R infoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.MESSAGE, fn, messageSerializer);
    }

    /**
     * Logs a warning message at the WARN level.
     *
     * @param message The warning message to log
     */
    public final void warn(String message) {
        logInternal(LogTypeEnum.WARN, message);
    }

    /**
     * Executes a function and logs its result at the WARN level.
     * Useful for operations that complete successfully but with concerning outcomes.
     *
     * @param <R>               The return type of the function
     * @param fn                The function to execute
     * @param messageSerializer Function to convert the result to a warning message
     * @return The result of the executed function
     */
    public final <R> R warnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.WARN, fn, messageSerializer);
    }

    /**
     * Logs an error message at the ERROR level.
     *
     * @param message The error message to log
     */
    public final void error(String message) {
        logInternal(LogTypeEnum.ERROR, message);
    }

    /**
     * Executes a function and logs its result at the ERROR level.
     * Useful for operations that fail or produce error conditions while still returning a value.
     *
     * @param <R>               The return type of the function
     * @param fn                The function to execute
     * @param messageSerializer Function to convert the result to an error message
     * @return The result of the executed function
     */
    public final <R> R errorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.ERROR, fn, messageSerializer);
    }

    /**
     * Abstract method that subclasses must implement to provide the logging context.
     * The context contains essential information including:
     * - Block information (ID, metadata)
     * - Current log sequence ID
     * - Buffer for storing log entries
     *
     * @return The VFLBlockContext containing all necessary logging state
     */
    protected abstract VFLBlockContext getContext();
}