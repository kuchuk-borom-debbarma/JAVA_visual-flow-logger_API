package dev.kuku.vfl.vfl_base;

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
    public void close(String endMessage) {
        ctx.buffer.pushLogEndToBuffer(ctx.blockInfo.getId(), endMessage);
    }

    @Override
    protected void setCurrentLogId(String newLogId) {
        ctx.currentLogId = newLogId;
    }

    @Override
    protected VFLBuffer getBuffer() {
        return ctx.buffer;
    }

    @Override
    protected String getCurrentLogId() {
        return ctx.currentLogId;
    }

    @Override
    protected String getBlockId() {
        return ctx.blockInfo.getId();
    }

    @Override
    protected void ensureBlockStarted() {
        if (blockStarted.compareAndSet(false, true)) {
            ctx.buffer.pushLogStartToBuffer(ctx.blockInfo.getId());
        }
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