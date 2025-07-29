package dev.kuku.vfl.core.fluent_api.callable.steps.runnable;

import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RunSubBlockStep {
    private final VFLCallable vfl;
    private final Runnable runnable;

    public RunBlockWithNameStep withBlockName(String blockName) {
        return new RunBlockWithNameStep(blockName, vfl, runnable);
    }
}
