package dev.kuku.vfl.core.fluent_api.fn;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.CallSubBlockStartStep;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.CompletableFutureSubBlockStartStep;
import dev.kuku.vfl.core.fluent_api.fn.steps.sub_block_start_step.ConsumerSubBlockStartStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.function.Consumer;
import java.util.function.Function;

public class FluentVFLFn extends FluentVFL {
    private final VFLFn vfl;

    public FluentVFLFn(VFLFn logger) {
        super(logger);
        this.vfl = logger;
    }

    public <R> CallSubBlockStartStep<R> startSubBlock(Function<VFLFn, R> fn) {
        return new CallSubBlockStartStep<>(fn, vfl);
    }

    public ConsumerSubBlockStartStep startSubBlock(Consumer<VFLFn> consumer) {
        return new ConsumerSubBlockStartStep(consumer, vfl);
    }

    public <R> CompletableFutureSubBlockStartStep<R> startAsyncSubBlock(Function<VFLFn, R> fn) {
        return new CompletableFutureSubBlockStartStep<>(fn, vfl);
    }

    public CompletableFutureSubBlockStartStep<Void> startAsyncSubBlock(Consumer<VFLFn> consumer) {
        return new CompletableFutureSubBlockStartStep<>(l -> {
            consumer.accept(l);
            return null;
        }, vfl);
    }
}