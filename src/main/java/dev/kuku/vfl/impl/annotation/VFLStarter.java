package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Supplier;

/**
 * Provides methods to start Logger for scenarios such as operation, continue from block & event listener.
 * It is required to use a starter to start logger because it needs to setup thread local variables.
 * In future, @RootBlock will be introduced as an alternative to StartRootBlock if user wants to use annotation
 */
public class VFLStarter {
    /**
     * Start a new flow
     *
     * @param blockName name of the flow
     * @param supplier  the method to run
     * @return return value of supplier
     */
    public static <R> R StartRootBlock(String blockName, Supplier<R> supplier) {
        //TODO throw exception if operation is already running i guess?
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        ThreadContextManager.CleanThreadVariables();
        Block rootBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, null, VFLInitializer.VFLAnnotationConfig.buffer);
        ThreadContextManager.InitializeStackWithBlock(rootBlock);
        R r;
        try {
            r = supplier.get();
            return r;
        } catch (Exception e) {
            Log.Error("Exception: {}-{})", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ThreadContextManager.CloseAndPopCurrentContext(null);
            //Safety clean up. Should not be required
            ThreadContextManager.CleanThreadVariables();
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    /**
     * Continue tracing from a block received from another service or process.
     * Use this when you receive a Block object (via HTTP headers, message payload, etc.)
     * from an upstream service and want to continue the distributed trace in your service.
     *
     * <p>This method:
     * <ul>
     *   <li>Initializes the current thread's VFL context with the received block</li>
     *   <li>Maintains the parent-child relationship in the distributed trace</li>
     *   <li>Ensures proper cleanup of thread-local variables</li>
     * </ul>
     *
     * <p>Typical usage pattern:
     * <pre>{@code
     * // Extract block from HTTP request headers or body
     * Block receivedBlock = deserialize(request.getHeader("trace-block"));
     *
     * return VFLStarter.ContinueFromBlock(receivedBlock, () -> {
     *     // Your service logic here - all @SubBlock methods and Log.* calls
     *     // will be part of the continued distributed trace
     *     return processUserRequest();
     * });
     * }</pre>
     *
     * @param continuationBlock The block received from upstream service/process
     * @param supplier          The method to execute within this trace context
     * @param <R>               Return type of the supplier
     * @return Result of the supplier execution
     */
    public static <R> R ContinueFromBlock(Block continuationBlock, Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        ThreadContextManager.CleanThreadVariables();
        ThreadContextManager.InitializeStackWithBlock(continuationBlock);
        R r;
        try {
            r = supplier.get();
            return r;
        } catch (Exception e) {
            Log.Error("Exception in continuation block: {}-{}", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ThreadContextManager.CloseAndPopCurrentContext(null);
            ThreadContextManager.CleanThreadVariables();
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    /**
     * Start event listener for the provided publisherBlock. Does not clear thread local variables in case both publisher and listener are running in same thread
     */
    public static <R> R StartEventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        Block eventListenerBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(eventListenerName, publisherBlock.block().getId(), VFLInitializer.VFLAnnotationConfig.buffer);

        VFLFlowHelper.CreateLogAndPush2Buffer(publisherBlock.block().getId(),
                null,
                message,
                eventListenerBlock.getId(),
                LogTypeBlockStartEnum.EVENT_LISTENER,
                VFLInitializer.VFLAnnotationConfig.buffer);
        ThreadContextManager.InitializeStackWithBlock(eventListenerBlock);

        R r;
        try {
            r = supplier.get();
            return r;
        } catch (Exception e) {
            Log.Error("Exception: {}-{})", e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            ThreadContextManager.CloseAndPopCurrentContext(null);
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    // Runnable variants that delegate to Supplier versions
    public static void StartRootBlock(String blockName, Runnable runnable) {
        StartRootBlock(blockName, () -> {
            runnable.run();
            return null;
        });
    }

    public static void ContinueFromBlock(Block continuationBlock, Runnable runnable) {
        ContinueFromBlock(continuationBlock, () -> {
            runnable.run();
            return null;
        });
    }

    public static void StartEventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Runnable runnable) {
        StartEventListener(publisherBlock, eventListenerName, message, () -> {
            runnable.run();
            return null;
        });
    }


}
