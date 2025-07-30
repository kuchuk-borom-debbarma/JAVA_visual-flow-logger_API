package dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.AsyncBlockExecutor;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

public class SetBlockNameNoEndAsync<R> extends BaseSetBlockName implements AsyncBlockExecutor<R> {

    private final Function<VFLFn, R> fn;

    public SetBlockNameNoEndAsync(String blockName, VFLFn vfl, Function<VFLFn, R> fn) {
        super(blockName, vfl);
        this.fn = fn;
    }

    @Override
    public SetBlockNameNoEndAsync withStartMessage(String startMessage, Object... args) {
        super.startMessage = FormatMessage(startMessage, args);
        return this;
    }

    @Override
    public CompletableFuture<R> startSecondaryJoining(Executor executor) {
        return CompletableFuture.supplyAsync(() ->
                vfl.callSecondaryJoiningBlock(blockName, startMessage, fn, null), executor);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return CompletableFuture.runAsync(() ->
                vfl.callSecondaryNonJoiningBlock(blockName, startMessage, fn::apply), executor);
    }
}
