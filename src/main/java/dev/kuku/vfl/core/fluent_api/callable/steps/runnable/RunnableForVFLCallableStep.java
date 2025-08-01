package dev.kuku.vfl.core.fluent_api.callable.steps.runnable;

import dev.kuku.vfl.core.fluent_api.base.steps.RunnableStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

public class RunnableForVFLCallableStep extends RunnableStep {
    private final VFLCallable vfl;

    public RunnableForVFLCallableStep(Runnable runnable, VFLCallable vfl) {
        super(runnable, vfl);
        this.vfl = vfl;
    }

    public RunBlockWithNameStep asBlock(String blockName) {
        return new RunBlockWithNameStep(blockName, vfl, runnable);
    }
}
