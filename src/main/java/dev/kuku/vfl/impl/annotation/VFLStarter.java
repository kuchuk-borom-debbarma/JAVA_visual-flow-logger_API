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
     * Start logger as a sub block, use this as the starter in scenarios where you need make a third party service call and as the service being called you want to continue from existing block that was sent as payload from by caller.
     *
     * @param block
     * @param supplier
     * @param <R>
     * @return
     */
    public static <R> R StartAsBlock(Block block, Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        ThreadContextManager.CleanThreadVariables();
        ThreadContextManager.InitializeStackWithBlock(block);
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
            ThreadContextManager.CleanThreadVariables();
            VFLInitializer.VFLAnnotationConfig.buffer.flush();
        }
    }

    /**
     * Start event listener for the provided publisherBlock
     */
    public static <R> R StartEventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Supplier<R> supplier) {
        if (VFLInitializer.isDisabled()) {
            return supplier.get();
        }

        ThreadContextManager.CleanThreadVariables();

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
            ThreadContextManager.CleanThreadVariables();
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

    public static void StartAsBlock(Block block, Runnable runnable) {
        StartAsBlock(block, () -> {
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
