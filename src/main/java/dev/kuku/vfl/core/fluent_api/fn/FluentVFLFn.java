package dev.kuku.vfl.core.fluent_api.fn;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.AsyncFnSubBlockStartStep;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.ConsumerAsyncSubBlockStartStep;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.ConsumerSubBlockStartStep;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.FnSubBlockStartStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Consumer;
import java.util.function.Function;

public class FluentVFLFn extends FluentVFL {
    private final VFLFn vfl;

    public FluentVFLFn(VFLFn logger) {
        super(logger);
        this.vfl = logger;
    }

    public <R> FnSubBlockStartStep<R> startSubBlock(Function<VFLFn, R> fn) {
        return new FnSubBlockStartStep<>(fn, vfl);
    }

    public ConsumerSubBlockStartStep startSubBlock(Consumer<VFLFn> consumer) {
        return new ConsumerSubBlockStartStep(consumer, vfl);
    }

    public <R> AsyncFnSubBlockStartStep<R> startAsyncSubBlock(Function<VFLFn, R> fn) {
        return new AsyncFnSubBlockStartStep<>(fn, vfl);
    }

    public ConsumerAsyncSubBlockStartStep<Void> startAsyncSubBlock(Consumer<VFLFn> consumer) {
        return new ConsumerAsyncSubBlockStartStep<>(consumer, vfl);
    }
}