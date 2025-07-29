package dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step;

import dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step.SetBlockNameAsync;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Function;

public class AsyncFnSubBlockStartStep<R> extends BaseSubBlockStartStep<R> {
    public AsyncFnSubBlockStartStep(Function<VFLFn, R> fn, VFLFn vfl) {
        super(fn, vfl);
    }

    public SetBlockNameAsync<R> withBlockName(String blockName) {
        return new SetBlockNameAsync<>(blockName, vfl, fn);
    }

}
