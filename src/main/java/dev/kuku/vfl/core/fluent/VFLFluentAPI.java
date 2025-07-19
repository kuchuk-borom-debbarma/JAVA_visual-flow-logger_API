package dev.kuku.vfl.core.fluent;

import dev.kuku.vfl.core.IVFL;

public class VFLFluentAPI {
    private final IVFL logger;

    public VFLFluentAPI(IVFL logger) {
        this.logger = logger;
    }

    public ITextStep text() {
        return new TextStep(this.logger);
    }
}