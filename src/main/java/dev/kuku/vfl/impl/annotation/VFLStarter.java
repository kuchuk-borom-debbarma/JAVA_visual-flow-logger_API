package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Supplier;

public class VFLStarter {
    public static <R> R StartRootBlock(String blockName, Supplier<R> supplier) {
        ThreadContextManager.CleanThreadVariables();
        Block rootBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(blockName, null, Configuration.INSTANCE.buffer);
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
            ThreadContextManager.CleanThreadVariables();
        }
    }

    /**
     * Start operation with the provided block as first block. Does not push the block to buffer.
     */
    public static <R> R StartOperationAsBlock(Block block, Supplier<R> supplier) {
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
        }
    }

    /**
     * Start event listener for the provided publisherBlock
     * @param publisherBlock
     * @param eventListenerName
     * @param message
     * @param supplier
     * @return
     * @param <R>
     */
    public static <R> R StartEventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Supplier<R> supplier) {
        ThreadContextManager.CleanThreadVariables();

        Block eventListenerBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(eventListenerName, publisherBlock.block().getId(), Configuration.INSTANCE.buffer);

        VFLFlowHelper.CreateLogAndPush2Buffer(publisherBlock.block().getId(),
                null,
                message,
                eventListenerBlock.getId(),
                LogTypeBlockStartEnum.EVENT_LISTENER,
                Configuration.INSTANCE.buffer);
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
        }
    }

    // Runnable variants that delegate to Supplier versions
    public static void StartRootBlock(String blockName, Runnable runnable) {
        StartRootBlock(blockName, () -> {
            runnable.run();
            return null;
        });
    }

    public static void StartOperationAsBlock(Block block, Runnable runnable) {
        StartOperationAsBlock(block, () -> {
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
