package dev.kuku.vfl;

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
    protected VFLBlockContext getContext() {
        return ctx;
    }
}