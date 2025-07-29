package dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step;

import dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step.SetBlockNameWithEndMsg;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Function;

public class FnSubBlockStartStep<R> extends BaseSubBlockStartStep<R> {
    public FnSubBlockStartStep(Function<VFLFn, R> fn, VFLFn vfl) {
        super(fn, vfl);
    }

    public SetBlockNameWithEndMsg<R> withBlockName(String blockName) {
        return new SetBlockNameWithEndMsg<>(blockName, vfl, fn);
    }
}
