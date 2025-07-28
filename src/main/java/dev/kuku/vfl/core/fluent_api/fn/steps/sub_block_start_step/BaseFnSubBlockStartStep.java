package dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step;

import dev.kuku.vfl.core.vfl_abstracts.VFLFn;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public abstract class BaseFnSubBlockStartStep<R> {
    protected final Function<VFLFn, R> fn;
    protected final VFLFn vfl;
}