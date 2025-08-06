package dev.kuku.vfl.impl.threadlocal.fluent.flient_steps;

import dev.kuku.vfl.core.fluent_api.steps.RunnableStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal.fluent.flient_steps.sub_block.AsSubBlockRunnableStep;

public class ThreadVFLRunnableStep extends RunnableStep {
    public ThreadVFLRunnableStep(Runnable runnable, VFL vfl) {
        super(runnable, vfl);
    }

    public AsSubBlockRunnableStep asSubBlock(String blockName) {
        return new AsSubBlockRunnableStep(blockName, runnable);
    }
}
