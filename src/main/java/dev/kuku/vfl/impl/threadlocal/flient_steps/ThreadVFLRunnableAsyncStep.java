package dev.kuku.vfl.impl.threadlocal.flient_steps;

import dev.kuku.vfl.core.fluent_api.base.steps.RunnableStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block.AsSubBlockRunnableAsyncStep;

public class ThreadVFLRunnableAsyncStep extends RunnableStep {

    public ThreadVFLRunnableAsyncStep(VFL vfl, Runnable supplier) {
        super(supplier, vfl);
    }

    public AsSubBlockRunnableAsyncStep asSubBlock(String blockName) {
        return new AsSubBlockRunnableAsyncStep(runnable, blockName);
    }
}
