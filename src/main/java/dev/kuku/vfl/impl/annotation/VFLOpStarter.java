package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.models.Block;

import java.util.function.Supplier;

public class VFLOpStarter {
    /**
     * Start a new operation, cleans thread values and starts a new root block
     *
     * @param blockName
     * @param runnable
     */
    public static void StartOperation(String blockName, Runnable runnable) {
        FlowHandler.StartOperation(blockName, () -> {
            runnable.run();
            return null;
        });
    }

    public static <R> R StartOperation(String blockName, Supplier<R> supplier) {
        return FlowHandler.StartOperation(blockName, supplier);
    }

    /**
     * Cleans up current thread and sets the passed block as the new starting block and starts operating from there.
     * Meant to be used for cross service logging where the block is passed as header or payload and the other service processes it.
     *
     * @param block
     * @param runnable
     */
    public static void ContinueAsBlock(Block block, Runnable runnable) {
        FlowHandler.ContinueAsBlock(block, () -> {
            runnable.run();
            return null;
        });
    }

    public static <R> R ContinueAsBlock(Block block, Supplier<R> supplier) {
        return FlowHandler.ContinueAsBlock(block, supplier);
    }

    public static void EventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Runnable runnable) {
        FlowHandler.startEventListener(publisherBlock, eventListenerName, message, runnable);
    }
}
