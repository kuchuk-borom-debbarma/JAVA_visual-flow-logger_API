package dev.kuku.vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class VFLBase extends VFL {
    VFLBlockContext ctx;
    private final AtomicBoolean blockStarted = new AtomicBoolean(false);

    protected VFLBase(VFLBlockContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected VFLBlockContext getContext() {
        return ctx;
    }

    static class Runner extends VFLRunner {
        public static <R> R call(String operationName, VFLBuffer buffer, Function<VFLBase, R> fn) {
            VFLBase logger = new VFLBase(initRootCtx(operationName, buffer));
            try {
                return VFLHelper.CallFnWithLogger(() -> fn.apply(logger), logger, null);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}