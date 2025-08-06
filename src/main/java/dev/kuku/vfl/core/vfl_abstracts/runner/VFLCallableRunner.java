package dev.kuku.vfl.core.vfl_abstracts.runner;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLFlowHelper;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.function.Supplier;

public abstract class VFLCallableRunner extends VFLRunner {
    public <R> R startVFL(String blockName, VFLBuffer buffer, Supplier<R> fn) {
        var context = initRootCtx(blockName, buffer);
        var logger = createRootLogger(context);
        try {
            return VFLFlowHelper.CallFnWithLogger(fn, logger, null);
        } finally {
            buffer.flushAndClose();
        }
    }

    public void startVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
        this.startVFL(blockName, buffer, () -> {
            runnable.run();
            return null;
        });
    }

    public void startEventListenerLogger(String eventListenerName, String eventStartMessage, VFLBuffer buffer, EventPublisherBlock eventData, Runnable r) {
        // Create the event listener block
        var eventListenerBlock = VFLFlowHelper.CreateBlockAndPush2Buffer(eventListenerName, eventData.block().getId(), buffer);
        // Create a log for event publisher block of type event listener
        var log = VFLFlowHelper.CreateLogAndPush2Buffer(eventData.block().getId(), null, eventStartMessage, eventListenerBlock.getId(), LogTypeBlockStartEnum.EVENT_LISTENER, buffer);
        VFLBlockContext eventListenerCtx = new VFLBlockContext(eventListenerBlock, buffer);
        var logger = createEventListenerLogger(eventListenerCtx);
        try {
            VFLFlowHelper.CallFnWithLogger(() -> {
                r.run();
                return null;
            }, logger, null);
        } finally {
            buffer.flushAndClose();
        }
    }

    protected abstract VFL createEventListenerLogger(VFLBlockContext eventListenerCtx);

    protected abstract VFL createRootLogger(VFLBlockContext rootCtx);

}
