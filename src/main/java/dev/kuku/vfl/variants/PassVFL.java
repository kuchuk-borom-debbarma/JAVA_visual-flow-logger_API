package dev.kuku.vfl.variants;

import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;
import dev.kuku.vfl.core.vfl_abstracts.runner.VFLFnRunner;

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
        public static Runner INSTANCE = new Runner();

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