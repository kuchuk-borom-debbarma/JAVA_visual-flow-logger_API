package dev.kuku.vfl.impl.supplied;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

public class FluentSuppliedVFL extends FluentVFL {
    private final VFLFn logger;

    public FluentSuppliedVFL(VFLFn logger) {
        super(logger);
        this.logger = logger;
    }
}
