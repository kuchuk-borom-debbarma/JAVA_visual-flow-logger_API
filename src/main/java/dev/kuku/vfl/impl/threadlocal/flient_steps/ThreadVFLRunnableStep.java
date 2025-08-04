package dev.kuku.vfl.impl.threadlocal.flient_steps;

import dev.kuku.vfl.core.fluent_api.base.steps.RunnableStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block.AsSubBlockRunnableStep;

public class ThreadVFLRunnableStep extends RunnableStep {
    public ThreadVFLRunnableStep(Runnable runnable, VFL vfl) {
        super(runnable, vfl);
    }

    public AsSubBlockRunnableStep asSubBlock(String blockName) {
        return new AsSubBlockRunnableStep(blockName, runnable);
    }
}
