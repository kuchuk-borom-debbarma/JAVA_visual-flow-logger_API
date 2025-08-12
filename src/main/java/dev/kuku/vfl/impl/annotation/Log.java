package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Main logging API for Visual Flow Logger (VFL) when using the annotation-based approach.
 *
 * <p>Provides static convenience methods for logging at different levels ({@code Info}, {@code Warn}, {@code Error}),
 * publishing events, and creating continuation blocks for distributed tracing.
 *
 * <p>All methods automatically link log entries to the current VFL block context
 * (root, sub-block, or event listener) so they appear as part of your execution flow.
 * If VFL is not initialized or disabled, these methods are no‑ops (or just execute the function without logging).
 *
 * <h2>Key use cases:</h2>
 * <ul>
 *   <li>Log a simple message linked to the current trace</li>
 *   <li>Wrap the execution of a lambda and automatically log before/after with messages containing results</li>
 *   <li>Publish an {@link EventPublisherBlock} to link async event processing to the originating trace</li>
 *   <li>Create a “continuation block” for cross-service or async messaging tracing</li>
 * </ul>
 *
 * <h2>Message formatting:</h2>
 * <p>Message templates support placeholders like {@code {0}}, {@code {1}}, etc. for method arguments,
 * and some methods also allow embedding the return value using {@code {r}} or {@code {return}}.
 */
public class Log {

    // Core VFL instance tied to current thread context and buffer
    static VFL INSTANCE = new VFL() {
        @Override
        protected BlockContext getContext() {
            return ThreadContextManager.GetCurrentBlockContext();
        }

        @Override
        protected VFLBuffer getBuffer() {
            return VFLInitializer.VFLAnnotationConfig.buffer;
        }
    };

    // -------------------- INFO --------------------

    /**
     * Log an informational message in the current VFL block.
     *
     * @param message message template
     * @param args    arguments for formatting
     */
    public static void Info(String message, Object... args) {
        if (!VFLInitializer.initialized) return;
        INSTANCE.info(VFLHelper.FormatMessage(message, args));
    }

    /**
     * Execute a supplier, then log an info message based on its return value.
     *
     * @param fn                supplier to execute
     * @param messageSerializer function to turn result into a log message
     * @param <R>               supplier return type
     */
    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();
        return INSTANCE.infoFn(fn, messageSerializer);
    }

    /**
     * Execute a supplier and log an info message with a template that can include the result.
     *
     * @param fn      supplier to run
     * @param message message template (use {r} to insert the return value)
     * @param args    args for formatting
     */
    public static <R> R InfoFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();
        Function<R, String> s = (r) -> VFLHelper.FormatMessage(message, VFLHelper.CombineArgsWithReturn(args, r));
        return INSTANCE.infoFn(fn, s);
    }

    /**
     * Run a {@link Runnable} and log an info message after completion.
     */
    public static void InfoFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }
        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> VFLHelper.FormatMessage(message, args);
        INSTANCE.infoFn(supplier, s);
    }

    // -------------------- WARN --------------------

    /** Same as {@link #Info(String, Object...)} but logs at WARN level. */
    public static void Warn(String message, Object... args) {
        if (!VFLInitializer.initialized) return;
        INSTANCE.warn(VFLHelper.FormatMessage(message, args));
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();
        return INSTANCE.warnFn(fn, messageSerializer);
    }

    public static <R> R WarnFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();
        Function<R, String> s = (r) -> VFLHelper.FormatMessage(message, VFLHelper.CombineArgsWithReturn(args, r));
        return INSTANCE.warnFn(fn, s);
    }

    public static void WarnFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }
        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> VFLHelper.FormatMessage(message, args);
        INSTANCE.warnFn(supplier, s);
    }

    // -------------------- ERROR --------------------

    /** Same as {@link #Info(String, Object...)} but logs at ERROR level. */
    public static void Error(String message, Object... args) {
        if (!VFLInitializer.initialized) return;
        INSTANCE.error(VFLHelper.FormatMessage(message, args));
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();
        return INSTANCE.errorFn(fn, messageSerializer);
    }

    public static <R> R ErrorFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();
        Function<R, String> s = (r) -> VFLHelper.FormatMessage(message, VFLHelper.CombineArgsWithReturn(args, r));
        return INSTANCE.errorFn(fn, s);
    }

    public static void ErrorFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }
        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> VFLHelper.FormatMessage(message, args);
        INSTANCE.errorFn(supplier, s);
    }

    // -------------------- EVENT PUBLISHING --------------------

    /**
     * Create and log an {@link EventPublisherBlock} which links an event to the current trace.
     * Use when producing messages or events that will be consumed later.
     */
    public static EventPublisherBlock Publish(String publisherName, String message) {
        if (!VFLInitializer.initialized) return null;
        return INSTANCE.publish(publisherName, message);
    }

    public static EventPublisherBlock Publish(String publisherName, String message, Object... args) {
        if (!VFLInitializer.initialized) return null;
        return INSTANCE.publish(publisherName, VFLHelper.FormatMessage(message, args));
    }

    /** Overload for when you have no start message. */
    public static EventPublisherBlock Publish(String publisherName) {
        if (!VFLInitializer.initialized) return null;
        return INSTANCE.publish(publisherName, "");
    }

    // -------------------- CONTINUATION BLOCKS --------------------

    /**
     * Create a detached continuation block for cross-service or async tracing.
     *
     * <p>The returned {@link Block} is <b>not</b> pushed to the current thread —
     * it’s meant to be sent to another external service, which can then use
     * {@link VFLStarter#ContinueFromBlock(Block, Supplier)} to continue the trace.
     *
     * <p><b>Typical uses:</b> passing trace info in HTTP headers, message payloads, or background jobs.
     */
    public static <R> R CreateContinuationBlock(String blockName, String startMessage, Function<Block, R> fn) {
        if (!VFLInitializer.initialized) return fn.apply(null);

        BlockContext currentContext = ThreadContextManager.GetCurrentBlockContext();
        if (currentContext == null) {
            throw new IllegalStateException(
                    "Cannot create continuation block: no active VFL context. " +
                            "Ensure you're inside a VFL root block or sub-block."
            );
        }

        Block detachedBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName,
                currentContext.blockInfo.getId(),
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                currentContext.blockInfo.getId(),
                currentContext.currentLogId,
                startMessage,
                detachedBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        currentContext.currentLogId = subBlockStartLog.getId();

        try {
            return fn.apply(detachedBlock);
        } catch (Exception e) {
            Log.Error("Exception in continuation block '{}': {} - {}",
                    blockName, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            // Future: consider adding sub-block end log here
        }
    }

    /** Overload with formatted start message. */
    public static <R> R CreateContinuationBlock(String blockName, String startMessage, Object[] args, Function<Block, R> fn) {
        return CreateContinuationBlock(blockName, VFLHelper.FormatMessage(startMessage, args), fn);
    }

    /** Overload with no start message. */
    public static <R> R CreateContinuationBlock(String blockName, Function<Block, R> fn) {
        return CreateContinuationBlock(blockName, "", fn);
    }

    /** Void version accepting a {@link Consumer}. */
    public static void CreateContinuationBlock(String blockName, String startMessage, Consumer<Block> consumer) {
        CreateContinuationBlock(blockName, startMessage, block -> {
            consumer.accept(block);
            return null;
        });
    }

    /** Void version with formatted message. */
    public static void CreateContinuationBlock(String blockName, String startMessage, Object[] args, Consumer<Block> consumer) {
        CreateContinuationBlock(blockName, VFLHelper.FormatMessage(startMessage, args), consumer);
    }

    /** Void version with no message. */
    public static void CreateContinuationBlock(String blockName, Consumer<Block> consumer) {
        CreateContinuationBlock(blockName, "", consumer);
    }
}
