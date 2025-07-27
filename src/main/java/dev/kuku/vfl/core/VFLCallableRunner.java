package dev.kuku.vfl.core;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.concurrent.Callable;

//TODO create VFL fn version with diffeerent start version
public abstract class VFLCallableRunner {

    protected static VFLBlockContext initRootCtx(String operationName, VFLBuffer buffer) {
        Block rootBlock = VFL.VFLHelper.CreateBlockAndPush2Buffer(operationName, null, buffer);
        return new VFLBlockContext(rootBlock, buffer);
    }

    public <R> R StartVFL(String blockName, VFLBuffer buffer, Callable<R> fn) {
        var context = initRootCtx(blockName, buffer);
        var logger = createRootLogger(context);
        try {
            return VFL.VFLHelper.CallFnWithLogger(fn, logger, null);
        } finally {
            buffer.flushAndClose();
        }
    }

    public void RunEventListener(String eventListenerName, String eventStartMessage, VFLBuffer buffer, EventPublisherBlock eventData, Runnable r) {
        //Create the event listener block
        var eventListenerBlock = VFL.VFLHelper.CreateBlockAndPush2Buffer(eventListenerName, eventData.block().getId(), buffer);
        //Create a log for event publisher block of type event listener
        var log = VFL.VFLHelper.CreateLogAndPush2Buffer(eventData.block().getId(), null, eventStartMessage, eventListenerBlock.getId(), LogTypeBlockStartEnum.EVENT_LISTENER, buffer);
        VFLBlockContext eventListenerCtx = new VFLBlockContext(eventListenerBlock, buffer);
        var logger = createEventListenerLogger(eventListenerCtx);
        try {
            VFL.VFLHelper.CallFnWithLogger(() -> {
                r.run();
                return null;
            }, logger, null);
        } finally {
            buffer.flushAndClose();
        }
    }

    /**
     * Create the respective root logger based on block name and buffer.
     */
    protected abstract VFL createRootLogger(VFLBlockContext rootCtx);

    /**
     * Create logger for event listener.
     */
    protected abstract VFL createEventListenerLogger(VFLBlockContext eventListenerCtx);

    public abstract VFLCallableRunner getRunner();
}