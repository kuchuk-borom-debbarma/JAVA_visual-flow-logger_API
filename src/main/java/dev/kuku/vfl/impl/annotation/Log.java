package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Log {
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

    // ================ INFO METHODS ================
    public static void Info(String message, Object... args) {
        if (!VFLInitializer.initialized) return;
        INSTANCE.info(Util.FormatMessage(message, args));
    }

    public static <R> R InfoFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();
        return INSTANCE.infoFn(fn, messageSerializer);
    }

    public static <R> R InfoFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();
        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
        return INSTANCE.infoFn(fn, s);
    }

    public static void InfoFn(Runnable runnable, String message, Object... args) {
        if (!VFLInitializer.initialized) {
            runnable.run();
            return;
        }

        Supplier<Void> supplier = () -> {
            runnable.run();
            return null;
        };
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        INSTANCE.infoFn(supplier, s);
    }

    // ================ WARN METHODS ================
    public static void Warn(String message, Object... args) {
        if (!VFLInitializer.initialized) return;

        INSTANCE.warn(Util.FormatMessage(message, args));
    }

    public static <R> R WarnFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();

        return INSTANCE.warnFn(fn, messageSerializer);
    }

    public static <R> R WarnFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();

        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
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
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        INSTANCE.warnFn(supplier, s);
    }

    // ================ ERROR METHODS ================
    public static void Error(String message, Object... args) {
        if (!VFLInitializer.initialized) return;

        INSTANCE.error(Util.FormatMessage(message, args));
    }

    public static <R> R ErrorFn(Supplier<R> fn, Function<R, String> messageSerializer) {
        if (!VFLInitializer.initialized) return fn.get();

        return INSTANCE.errorFn(fn, messageSerializer);
    }

    public static <R> R ErrorFn(Supplier<R> fn, String message, Object... args) {
        if (!VFLInitializer.initialized) return fn.get();

        Function<R, String> s = (r) -> Util.FormatMessage(message, Util.CombineArgsWithReturn(args, r));
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
        Function<Void, String> s = (r) -> Util.FormatMessage(message, args);
        INSTANCE.errorFn(supplier, s);
    }

    // ================ PUBLISH EVENT METHODS ================
    public static EventPublisherBlock Publish(String publisherName, String message) {
        if (!VFLInitializer.initialized) return null;
        return INSTANCE.publish(publisherName, message);
    }

    public static EventPublisherBlock Publish(String publisherName, String message, Object... args) {
        if (!VFLInitializer.initialized) return null;
        return INSTANCE.publish(publisherName, Util.FormatMessage(message, args));
    }

    // Optional convenience overload if you sometimes don't have a message
    public static EventPublisherBlock Publish(String publisherName) {
        if (!VFLInitializer.initialized) return null;
        return INSTANCE.publish(publisherName, "");
    }

    // ================ CREATE CONTINUATION BLOCK METHODS ================

    /**
     * Creates a detached sub-block that can be serialized and sent to external services
     * for distributed tracing continuation. This block is NOT added to the current thread's
     * context stack - it's designed to be passed as payload (headers/body) to other services
     * which can then use {@link VFLStarter#ContinueFromBlock(Block, Supplier)} to continue the trace.
     *
     * <p>Use this when you need to:
     * <ul>
     *   <li>Make HTTP calls to other services and want them to continue your trace</li>
     *   <li>Send messages to queues/topics with trace context</li>
     *   <li>Spawn processes that should inherit trace context</li>
     * </ul>
     *
     * <p>Example usage:
     * <pre>{@code
     * Block continuationBlock = Log.CreateContinuationBlock(
     *     "UserService.GetUser",
     *     "Calling user service",
     *     block -> {
     *         // Add block to HTTP headers or request body
     *         return httpClient.call("/users/123", headers.with("trace-block", serialize(block)));
     *     }
     * );
     * }</pre>
     *
     * @param blockName    Name of the detached block for tracing
     * @param startMessage Message logged when the block starts
     * @param fn           Function that receives the detached block and handles the external call
     * @param <R>          Return type of the function
     * @return Result of the function execution
     */
    public static <R> R CreateContinuationBlock(String blockName, String startMessage, Function<Block, R> fn) {
        if (!VFLInitializer.initialized) return fn.apply(null);

        BlockContext currentContext = ThreadContextManager.GetCurrentBlockContext();
        if (currentContext == null) {
            throw new IllegalStateException("Cannot create continuation block: no active VFL context. Ensure you're within a VFL root block or sub-block.");
        }

        // Create the detached sub-block
        Block detachedBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName,
                currentContext.blockInfo.getId(),
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        // Log the start of this detached block
        SubBlockStartLog subBlockStartLog = VFLFlowHelper.CreateLogAndPush2Buffer(
                currentContext.blockInfo.getId(),
                currentContext.currentLogId,
                startMessage,
                detachedBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY,
                VFLInitializer.VFLAnnotationConfig.buffer
        );

        // Update current log ID to maintain the chain
        currentContext.currentLogId = subBlockStartLog.getId();

        R result;
        try {
            // Execute the function with the detached block
            // Note: We deliberately do NOT push this block to the thread stack nor do we call ensureStarted because it needs to be started by receiver
            result = fn.apply(detachedBlock);
            return result;
        } catch (Exception e) {
            Log.Error("Exception in continuation block '{}': {} - {}",
                    blockName, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            // TODO: Add sub-block end log to update timestamp
            System.out.println("TODO");
        }
    }

    /**
     * Creates a continuation block with a formatted start message.
     *
     * @param blockName    Name of the detached block for tracing
     * @param startMessage Message template with placeholders
     * @param args         Arguments to format into the message
     * @param fn           Function that receives the detached block and handles the external call
     * @param <R>          Return type of the function
     * @return Result of the function execution
     */
    public static <R> R CreateContinuationBlock(String blockName, String startMessage, Object[] args, Function<Block, R> fn) {
        return CreateContinuationBlock(blockName, Util.FormatMessage(startMessage, args), fn);
    }

    /**
     * Creates a continuation block with no start message logged.
     *
     * @param blockName Name of the detached block for tracing
     * @param fn        Function that receives the detached block and handles the external call
     * @param <R>       Return type of the function
     * @return Result of the function execution
     */
    public static <R> R CreateContinuationBlock(String blockName, Function<Block, R> fn) {
        return CreateContinuationBlock(blockName, "", fn);
    }

    /**
     * Creates a continuation block for void operations (Runnable).
     *
     * @param blockName    Name of the detached block for tracing
     * @param startMessage Message logged when the block starts
     * @param consumer     Consumer that receives the detached block and handles the external call
     */
    public static void CreateContinuationBlock(String blockName, String startMessage, Consumer<Block> consumer) {
        CreateContinuationBlock(blockName, startMessage, block -> {
            consumer.accept(block);
            return null;
        });
    }

    /**
     * Creates a continuation block for void operations with formatted start message.
     *
     * @param blockName    Name of the detached block for tracing
     * @param startMessage Message template with placeholders
     * @param args         Arguments to format into the message
     * @param consumer     Consumer that receives the detached block and handles the external call
     */
    public static void CreateContinuationBlock(String blockName, String startMessage, Object[] args, Consumer<Block> consumer) {
        CreateContinuationBlock(blockName, Util.FormatMessage(startMessage, args), consumer);
    }

    /**
     * Creates a continuation block for void operations with no start message.
     *
     * @param blockName Name of the detached block for tracing
     * @param consumer  Consumer that receives the detached block and handles the external call
     */
    public static void CreateContinuationBlock(String blockName, Consumer<Block> consumer) {
        CreateContinuationBlock(blockName, "", consumer);
    }
}
