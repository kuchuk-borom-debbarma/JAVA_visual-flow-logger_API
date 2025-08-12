package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Supplier;

/**
 * Utility class to **start and continue VFL (Visual Flow Logger) tracing** within your application.
 *
 * <p>This is the entry point for wrapping your business logic so that it runs
 * within a properly initialised logging context.
 * It ensures thread-local variables are set up, blocks are pushed/popped
 * in sequence, and logs are flushed at the end.
 *
 * <p>Common scenarios:
 * <ul>
 *     <li><b>StartRootBlock</b> – Begin a new trace for a root operation</li>
 *     <li><b>ContinueFromBlock</b> – Continue an existing trace received from another service</li>
 *     <li><b>StartEventListener</b> – Trace asynchronous work triggered by events/messages</li>
 * </ul>
 *
 * <p>**Note:** If VFL is disabled by configuration, these methods execute your logic without any tracing overhead.
 */
public class VFLStarter {

    /**
     * Start a new traceable root block for your operation.
     *
     * <p>Use this at the top of your service/method to start a fresh VFL trace.
     * All nested {@code @SubBlock} calls and logs will belong to this root trace.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * return VFLStarter.StartRootBlock("ProcessOrder", () -> {
     *     validateOrder();
     *     saveOrder();
     *     return "OrderID-123";
     * });
     * }</pre>
     *
     * @param blockName Logical name for the root block (operation name)
     * @param supplier  Code to execute within the trace context
     * @return The result from the supplied code
     */
    public static <R> R StartRootBlock(String blockName, Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        Block rootBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                blockName, null, VFLInitializer.VFLAnnotationConfig.buffer);

        ThreadContextManager.PushBlockToThreadLogStack(rootBlock);
        Log.INSTANCE.ensureBlockStarted();

        try {
            return supplier.get();
        } catch (Exception e) {
            Log.Error("Exception: {}-{})", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ThreadContextManager.PopCurrentStack(null);
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    /**
     * Continue tracing from a block received from another service/process.
     *
     * <p>Use for **distributed tracing** when your service is called by another system
     * that already has a VFL trace in progress.
     * Pass the {@link Block} object (e.g., from HTTP headers or message) so VFL
     * can maintain the parent-child relationship in logs.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * Block traceBlock = deserialize(request.getHeader("vfl-block"));
     * return VFLStarter.ContinueFromBlock(traceBlock, () -> handleRequest());
     * }</pre>
     *
     * @param continuationBlock The block object from upstream service
     * @param supplier          Your logic to execute inside the continued trace
     * @return The result from your code
     */
    public static <R> R ContinueFromBlock(Block continuationBlock, Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        ThreadContextManager.PushBlockToThreadLogStack(continuationBlock);
        Log.INSTANCE.ensureBlockStarted();

        try {
            return supplier.get();
        } catch (Exception e) {
            Log.Error("Exception in continuation block: {}-{}", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ThreadContextManager.PopCurrentStack(null);
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    /**
     * Start tracing for an event listener/message consumer.
     *
     * <p>Commonly used in async or pub/sub systems to log event handling
     * as part of the broader trace from the original publisher.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * VFLStarter.StartEventListener(publisherBlock, "OrderEventListener",
     *     "Received Order Created Event",
     *     () -> processOrderEvent()
     * );
     * }</pre>
     *
     * @param publisherBlock    The event publisher block (links listener to original trace)
     * @param eventListenerName Logical name for this event listener
     * @param message           Optional log message for the listener start
     * @param supplier          Code to execute to handle the event
     * @return The result from your code
     */
    public static <R> R StartEventListener(EventPublisherBlock publisherBlock,
                                           String eventListenerName,
                                           String message,
                                           Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        Block eventListenerBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(
                eventListenerName,
                publisherBlock.block().getId(),
                VFLInitializer.VFLAnnotationConfig.buffer);

        VFLFlowHelper.CreateLogAndPush2Buffer(
                publisherBlock.block().getId(),
                null,
                message,
                eventListenerBlock.getId(),
                LogTypeBlockStartEnum.EVENT_LISTENER,
                VFLInitializer.VFLAnnotationConfig.buffer);

        ThreadContextManager.PushBlockToThreadLogStack(eventListenerBlock);
        Log.INSTANCE.ensureBlockStarted();

        try {
            return supplier.get();
        } catch (Exception e) {
            Log.Error("Exception: {}-{})", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ThreadContextManager.PopCurrentStack(null);
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    /**
     * Overloaded shorter form that starts an event listener trace without an initial message.
     */
    public static <R> R StartEventListener(EventPublisherBlock publisherBlock,
                                           String eventListenerName,
                                           Supplier<R> supplier) {
        return StartEventListener(publisherBlock, eventListenerName, null, supplier);
    }

    // 🚀 Runnable variants (no return value needed) — these simply wrap the Supplier versions

    /** Runnable version of {@link #StartRootBlock(String, Supplier)} */
    public static void StartRootBlock(String blockName, Runnable runnable) {
        StartRootBlock(blockName, () -> {
            runnable.run();
            return null;
        });
    }

    /** Runnable version of {@link #ContinueFromBlock(Block, Supplier)} */
    public static void ContinueFromBlock(Block continuationBlock, Runnable runnable) {
        ContinueFromBlock(continuationBlock, () -> {
            runnable.run();
            return null;
        });
    }

    /** Runnable version of {@link #StartEventListener(EventPublisherBlock, String, String, Supplier)} */
    public static void StartEventListener(EventPublisherBlock publisherBlock,
                                          String eventListenerName,
                                          String message,
                                          Runnable runnable) {
        StartEventListener(publisherBlock, eventListenerName, message, () -> {
            runnable.run();
            return null;
        });
    }

    /** Runnable version of {@link #StartEventListener(EventPublisherBlock, String, Supplier)} */
    public static void StartEventListener(EventPublisherBlock publisherBlock,
                                          String eventListenerName,
                                          Runnable runnable) {
        StartEventListener(publisherBlock, eventListenerName, null, () -> {
            runnable.run();
            return null;
        });
    }
}
