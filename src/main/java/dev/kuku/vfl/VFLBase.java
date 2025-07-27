package dev.kuku.vfl;

import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.concurrent.atomic.AtomicBoolean;

public class VFLBase extends VFL {
    private final AtomicBoolean blockStarted = new AtomicBoolean(false);
    VFLBlockContext ctx;

    protected VFLBase(VFLBlockContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected VFLBlockContext getContext() {
        return ctx;
    }
}