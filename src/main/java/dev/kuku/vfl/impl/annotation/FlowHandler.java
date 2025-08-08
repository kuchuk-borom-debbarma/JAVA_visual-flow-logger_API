package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.function.Supplier;

public class FlowHandler {
    public static void startEventListener(EventPublisherBlock publisherBlock, String eventListenerName, String message, Runnable runnable) {
        var eventListenerBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(eventListenerName, publisherBlock.block().getId(), ThreadContextManager.AnnotationBuffer);
        var eventListenerStart = VFLFlowHelper.CreateLogAndPush2Buffer(publisherBlock.block().getId(),
                null, message, eventListenerBlock.getId(),
                LogTypeBlockStartEnum.EVENT_LISTENER, ThreadContextManager.AnnotationBuffer);
        ThreadContextManager.startSubBlockCleanFrom(eventListenerBlock);
        try {
            runnable.run();
        } finally {
            ThreadContextManager.closeCurrentContext(null);
        }
    }

    public static <R> R ContinueAsBlock(Block block, Supplier<R> supplier) {
        ThreadContextManager.startSubBlockCleanFrom(block);
        R result;
        try {
            result = supplier.get();
            return result;
        } finally {
            ThreadContextManager.closeCurrentContext(null);
        }
    }

    public static <R> R StartOperation(String blockName, Supplier<R> supplier) {
        ThreadContextManager.startRootBlock(blockName);
        R result = null;
        try {
            result = supplier.get();
            return result;
        } finally {
            ThreadContextManager.closeCurrentContext(result);
        }
    }
}
