package dev.kuku.vfl.impl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;
import dev.kuku.vfl.core.vfl_abstracts.runner.VFLFnRunner;

import java.util.function.Consumer;
import java.util.function.Function;

public class PassVFL extends VFLFn {
    private final VFLBlockContext ctx;

    protected PassVFL(VFLBlockContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected VFLFn getLogger() {
        return this;
    }

    @Override
    protected VFLBlockContext getContext() {
        return ctx;
    }

    public static class Runner extends VFLFnRunner {
        private static Runner INSTANCE = new Runner();

        public static <R> R StartVFL(String blockName, VFLBuffer buffer, Function<VFLFn, R> fn) {
            return INSTANCE.startVFL(blockName, buffer, fn);
        }

        public static void StartVFL(String blockName, VFLBuffer buffer, Consumer<VFLFn> fn) {
            INSTANCE.StartVFL(blockName, buffer, fn);
        }

        public static void StartEventListenerLogger(String eventListenerName, String eventStartMessage, VFLBuffer buffer, EventPublisherBlock eventData, Consumer<VFLFn> r) {
            INSTANCE.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventData, r);
        }


        @Override
        protected VFLFn createRootLogger(VFLBlockContext rootCtx) {
            return new PassVFL(rootCtx);
        }

        @Override
        protected VFLFn createEventListenerLogger(VFLBlockContext eventListenerCtx) {
            return new PassVFL(eventListenerCtx);
        }
    }
}