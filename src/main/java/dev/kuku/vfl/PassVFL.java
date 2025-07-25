package dev.kuku.vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

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

    static class Runner extends VFLRunner {
        public static <R> R call(String operationName, VFLBuffer buffer, Function<PassVFL, R> fn) {
            PassVFL logger = new PassVFL(initRootCtx(operationName, buffer));
            try {
                return VFLHelper.CallFnWithLogger(() -> fn.apply(logger), logger, null);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}