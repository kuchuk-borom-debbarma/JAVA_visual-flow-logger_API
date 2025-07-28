package dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.AsyncBlockExecutor;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockCallableEndMessage;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockExecutor;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class SetBlockNameWithEndMsg<R> extends BaseSetBlockName implements BlockCallableEndMessage<R>, BlockExecutor<R>, AsyncBlockExecutor<R> {
    protected final Function<VFLFn, R> fn;
    protected Function<R, String> endMessage;

    public SetBlockNameWithEndMsg(String blockName, VFLFn vfl, Function<VFLFn, R> fn) {
        super(blockName, vfl);
        this.fn = fn;
    }


    @Override
    public SetBlockNameWithEndMsg<R> withStartMessage(String startMessage) {
        super.startMessage = startMessage;
        return this;
    }

    @Override
    public R startPrimary() {
        return vfl.callPrimarySubBlock(blockName, startMessage, fn, endMessage);
    }

    @Override
    public CompletableFuture<R> startSecondaryJoining(Executor executor) {
        return CompletableFuture.supplyAsync(() ->
                vfl.callSecondaryJoiningBlock(blockName, startMessage, fn, endMessage), executor);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return CompletableFuture.runAsync(() ->
                vfl.callSecondaryNonJoiningBlock(blockName, startMessage, fn::apply), executor);
    }

    @Override
    public BlockCallableEndMessage<R> withEndMessageMapper(Function<R, String> endMessageSerializer) {
        this.endMessage = endMessageSerializer;
        return this;
    }
}