package dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step;

import dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step.SetBlockNameNoEndAsync;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Function;

public class ConsumerCompletableFutureSubBlockStartStep extends BaseFnSubBlockStartStep<Void> {
    public ConsumerCompletableFutureSubBlockStartStep(Function<VFLFn, Void> fn, VFLFn vfl) {
        super(fn, vfl);
    }

    public SetBlockNameNoEndAsync withBlockName(String name) {

    }
}
