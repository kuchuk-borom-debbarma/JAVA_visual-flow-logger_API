package dev.kuku.vfl.passthrough.fluent;

import dev.kuku.vfl.core.fluent.VFLFluentAPI;
import dev.kuku.vfl.passthrough.IPassthroughVFL;

public class PassthroughFluentAPI {
    public final VFLFluentAPI text;
    private final IPassthroughVFL logger;

    public PassthroughFluentAPI(IPassthroughVFL loggerInstance) {
        this.logger = loggerInstance;
        this.text = new VFLFluentAPI(loggerInstance);
    }

    public ISubBlockStartStep startSubBlock(String blockName) {
        return new SubBlockStartStep(logger, blockName);
    }
}