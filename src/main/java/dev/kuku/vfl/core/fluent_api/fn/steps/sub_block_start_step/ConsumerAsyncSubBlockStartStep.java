package dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step;

import dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step.SetBlockNameNoEndAsync;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Consumer;

public class ConsumerAsyncSubBlockStartStep<R> extends BaseSubBlockStartStep<R> {

    public ConsumerAsyncSubBlockStartStep(Consumer<VFLFn> fn, VFLFn vfl) {
        super((l) -> {
            fn.accept(l);
            return null;
        }, vfl);
    }

    public SetBlockNameNoEndAsync<R> withBlockName(String name) {
        return new SetBlockNameNoEndAsync<>(name, super.vfl, super.fn);
    }
}
