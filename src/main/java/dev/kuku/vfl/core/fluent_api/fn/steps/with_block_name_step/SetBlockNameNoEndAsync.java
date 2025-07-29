package dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.AsyncBlockExecutor;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class SetBlockNameNoEndAsync extends BaseSetBlockName implements AsyncBlockExecutor<Void> {
    protected final Consumer<VFLFn> fn;

    public SetBlockNameNoEndAsync(String blockName, VFLFn vfl, Consumer<VFLFn> fn) {
        super(blockName, vfl);
        this.fn = fn;
    }

    @Override
    public SetBlockNameNoEndAsync withStartMessage(String startMessage) {
        super.startMessage = startMessage;
        return this;
    }

    @Override
    public CompletableFuture<Void> startSecondaryJoining(Executor executor) {
        return CompletableFuture.supplyAsync(() ->
                vfl.callSecondaryJoiningBlock(blockName, startMessage, l -> {
                    fn.accept(l);
                    return null;
                }, null), executor);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return CompletableFuture.runAsync(() ->
                vfl.callSecondaryNonJoiningBlock(blockName, startMessage, fn::accept), executor);
    }
}
