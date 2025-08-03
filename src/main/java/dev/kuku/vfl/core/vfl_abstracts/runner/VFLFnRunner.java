package dev.kuku.vfl.core.vfl_abstracts.runner;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class VFLFnRunner extends VFLRunner {
    public <R> R startVFL(String blockName, VFLBuffer buffer, Function<VFLFn, R> fn) {
        var context = initRootCtx(blockName, buffer);
        var logger = createRootLogger(context);
        try {
            return VFL.VFLHelper.CallFnWithLogger(() -> fn.apply(logger), logger, null);
        } finally {
            buffer.flushAndClose();
        }
    }

    public void startVFL(String blockName, VFLBuffer buffer, Consumer<VFLFn> fn) {
        this.startVFL(blockName, buffer, (l) -> {
            fn.accept(l);
            return null;
        });
    }

    public void startEventListenerLogger(String eventListenerName, String eventStartMessage, VFLBuffer buffer, EventPublisherBlock eventData, Consumer<VFLFn> r) {
        // Create the event listener block
        var eventListenerBlock = VFL.VFLHelper.CreateBlockAndPush2Buffer(eventListenerName, eventData.block().getId(), buffer);
        // Create a log for event publisher block of type event listener
        var log = VFL.VFLHelper.CreateLogAndPush2Buffer(eventData.block().getId(), null, eventStartMessage, eventListenerBlock.getId(), LogTypeBlockStartEnum.EVENT_LISTENER, buffer);
        VFLBlockContext eventListenerCtx = new VFLBlockContext(eventListenerBlock, false, buffer);
        var logger = createEventListenerLogger(eventListenerCtx);
        try {
            VFL.VFLHelper.CallFnWithLogger(() -> {
                r.accept(logger);
                return null;
            }, logger, null);
        } finally {
            buffer.flushAndClose();
        }
    }

    protected abstract VFLFn createEventListenerLogger(VFLBlockContext eventListenerCtx);

    protected abstract VFLFn createRootLogger(VFLBlockContext rootCtx);

}