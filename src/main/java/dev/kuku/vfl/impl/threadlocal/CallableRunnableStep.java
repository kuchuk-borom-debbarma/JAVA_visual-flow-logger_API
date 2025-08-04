package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.fluent_api.base.steps.RunnableStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

public class CallableRunnableStep extends RunnableStep {
    public CallableRunnableStep(Runnable runnable, VFL vfl) {
        super(runnable, vfl);
    }

    public AsSubBlockRunnableStep asSubBlock(String blockName) {
        return new AsSubBlockRunnableStep(blockName, runnable);
    }
}
