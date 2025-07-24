package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

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
    public void close(String endMessage) {

    }

    @Override
    protected void setCurrentLogId(String newLogId) {

    }

    @Override
    protected VFLBuffer getBuffer() {
        return null;
    }

    @Override
    protected String getCurrentLogId() {
        return "";
    }

    @Override
    protected String getBlockInfo() {
        return "";
    }

    @Override
    protected void ensureBlockStarted() {

    }
}