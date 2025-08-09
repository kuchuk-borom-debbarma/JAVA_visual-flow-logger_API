package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Supplier;

public class VFLStarter {
    public static <R> R StartOperation(String blockName, Supplier<R> supplier) {
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
            Log.INSTANCE.close(null);
            ThreadContextManager.CleanThreadVariables();
        }
    }

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
            Log.INSTANCE.close(null);
            ThreadContextManager.CleanThreadVariables();
        }
    }

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
            Log.INSTANCE.close(null);
            ThreadContextManager.CleanThreadVariables();
        }
    }

    // Runnable variants that delegate to Supplier versions
    public static void StartOperation(String blockName, Runnable runnable) {
        StartOperation(blockName, () -> {
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
