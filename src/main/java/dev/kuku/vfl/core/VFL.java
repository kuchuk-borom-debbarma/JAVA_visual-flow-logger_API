package dev.kuku.vfl.core;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.dtos.BlockEndData;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.models.logs.enums.LogTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Base class for all Visual Flow Logger (VFL) implementations.
 *
 * <p>This abstract class provides the core logging, event publishing,
 * and block lifecycle handling for VFL traces. It is designed to be
 * subclassed by concrete logging entry points, such as
 * {@code Log} in the annotation-based VFL implementation.</p>
 *
 * <h2>Key Behaviors</h2>
 * <ul>
 *   <li>Ensures a block is "started" before writing logs to it</li>
 *   <li>Supports logging at INFO ({@link #info}), WARN ({@link #warn}), and ERROR ({@link #error}) levels</li>
 *   <li>Allows functional-style logging with {@code *Fn} variants that run a lambda and log its result</li>
 *   <li>Enables event publishing via {@link #publish(String, String)} which links producer and consumer traces</li>
 * </ul>
 *
 * <h2>Framework Notes</h2>
 * <ul>
 *   <li>Log entries are always scoped to the current {@link BlockContext} returned by {@link #getContext()}</li>
 *   <li>{@link #getBuffer()} determines where logs are temporarily staged (in-memory, async buffer, etc.)</li>
 *   <li>Public logging methods are final to maintain consistency; intended extensions are via context/buffer provision</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * A subclass provides the context and buffer:
 * <pre>{@code
 * static VFL INSTANCE = new VFL() {
 *     protected BlockContext getContext() {
 *         return ThreadContextManager.GetCurrentBlockContext();
 *     }
 *     protected VFLBuffer getBuffer() {
 *         return VFLInitializer.VFLAnnotationConfig.buffer;
 *     }
 * };
 *
 * // In user code inside a VFLStarter block:
 * Log.Info("Processing item {}", itemId);
 * }</pre>
 */
@Slf4j
public abstract class VFL {

    /**
     * Ensures the current block has been marked as started.
     * <p>If not yet started, pushes a "start" entry into the buffer with the current timestamp.
     * This must be done before any logs for the block are written.</p>
     */
    public final void ensureBlockStarted() {
        final BlockContext context = getContext();
        if (context.blockStarted.compareAndSet(false, true)) {
            final long startTimestamp = Instant.now().toEpochMilli();
            getBuffer().pushLogStartToBuffer(context.blockInfo.getId(), startTimestamp);
        }
    }

    /**
     * Closes the current block, pushing an "end" entry into the buffer
     * with an optional final message and timestamp.
     *
     * @param endMessage optional message describing why or how the block completed
     */
    public void close(String endMessage) {
        ensureBlockStarted();
        final BlockContext context = getContext();
        final long endTimestamp = Instant.now().toEpochMilli();
        getBuffer().pushLogEndToBuffer(context.blockInfo.getId(), new BlockEndData(endTimestamp, endMessage));
    }

    /**
     * Core logging method that ensures the block is started,
     * creates a log entry with the given type and message,
     * and updates the current log ID in context.
     */
    private void logInternal(LogTypeEnum type, String message) {
        ensureBlockStarted();
        final BlockContext context = getContext();
        final var createdLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                context.blockInfo.getId(),
                context.currentLogId,
                type,
                message,
                getBuffer()
        );
        context.currentLogId = createdLog.getId();
    }

    /**
     * Executes a function, then serializes and logs the result.
     * This is useful when the log message depends on the return value.
     */
    private <R> R logFnInternal(LogTypeEnum type, Supplier<R> fn, Function<R, String> messageSerializer) {
        final R result = fn.get();
        final String message = messageSerializer.apply(result);
        logInternal(type, message);
        return result;
    }

    // ========== PUBLIC LOGGING METHODS ==========

    /**
     * Log a message at INFO/MESSAGE level.
     */
    public final void info(String message) {
        logInternal(LogTypeEnum.MESSAGE, message);
    }

    /**
     * Executes a supplier and logs its result at MESSAGE level.
     * Allows result-based dynamic message generation.
     */
    public final <R> R infoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.MESSAGE, fn, messageSerializer);
    }

    /**
     * Log a warning message at WARN level.
     */
    public final void warn(String message) {
        logInternal(LogTypeEnum.WARN, message);
    }

    /**
     * Executes a supplier and logs its result at WARN level.
     */
    public final <R> R warnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.WARN, fn, messageSerializer);
    }

    /**
     * Executes a supplier and logs its result at ERROR level.
     */
    public final <R> R errorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        return logFnInternal(LogTypeEnum.ERROR, fn, messageSerializer);
    }

    /**
     * Log an error message at ERROR level.
     */
    public final void error(String message) {
        logInternal(LogTypeEnum.ERROR, message);
    }

    // ========== EVENT PUBLISHING ==========

    /**
     * Creates and logs an {@link EventPublisherBlock} linked to the current block.
     * <p>Used to track outgoing events/messages so they can be connected
     * to downstream processing in event listeners.</p>
     *
     * @param publisherName name of the publisher/event source
     * @param message       optional message describing the event
     * @return the created {@link EventPublisherBlock} representing this publish action
     */
    public final EventPublisherBlock publish(String publisherName, String message) {
        Block publisherBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                publisherName,
                getContext().blockInfo.getId(),
                getBuffer()
        );
        var publishLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                getContext().blockInfo.getId(),
                getContext().currentLogId,
                message,
                publisherBlock.getId(),
                LogTypeBlockStartEnum.PUBLISH_EVENT,
                getBuffer()
        );
        getContext().currentLogId = publishLog.getId();
        return new EventPublisherBlock(publisherBlock);
    }

    /**
     * @return The current logging {@link BlockContext}, provided by subclasses
     */
    protected abstract BlockContext getContext();

    /**
     * @return The {@link VFLBuffer} to receive log entries for this context
     */
    protected abstract VFLBuffer getBuffer();
}
