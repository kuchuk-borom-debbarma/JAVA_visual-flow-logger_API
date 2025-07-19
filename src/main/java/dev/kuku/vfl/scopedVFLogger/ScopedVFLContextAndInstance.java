package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.models.VFLBlockContext;

public class ScopedVFLContextAndInstance {
    public final VFLBlockContext scopedContext;
    public final ScopedVFL scopedInstance;

    public ScopedVFLContextAndInstance(VFLBlockContext scopedContext, ScopedVFL scopedInstance) {
        this.scopedContext = scopedContext;
        this.scopedInstance = scopedInstance;
    }
}
