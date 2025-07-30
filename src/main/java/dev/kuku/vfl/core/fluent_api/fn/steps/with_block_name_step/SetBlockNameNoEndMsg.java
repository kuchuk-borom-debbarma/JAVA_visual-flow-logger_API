package dev.kuku.vfl.core.fluent_api.fn.steps.with_block_name_step;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.AsyncBlockExecutor;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockExecutor;
import dev.kuku.vfl.core.vfl_abstracts.VFLFn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

public class SetBlockNameNoEndMsg extends BaseSetBlockName implements BlockExecutor<Void>, AsyncBlockExecutor<Void> {
    protected final Consumer<VFLFn> consumer;

    public SetBlockNameNoEndMsg(String blockName, VFLFn vfl, Consumer<VFLFn> consumer) {
        super(blockName, vfl);
        this.consumer = consumer;
    }

    @Override
    public SetBlockNameNoEndMsg withStartMessage(String startMessage, Object... args) {
        super.startMessage = FormatMessage(startMessage, args);
        return this;
    }

    @Override
    public Void startPrimary() {
        // Convert consumer to function for VFLFn compatibility
        Function<VFLFn, Void> fn = l -> {
            consumer.accept(l);
            return null;
        };
        vfl.callPrimarySubBlock(blockName, startMessage, fn, null);
        return null;
    }

    @Override
    public CompletableFuture<Void> startSecondaryJoining(Executor executor) {
        return CompletableFuture.runAsync(() -> {
            Function<VFLFn, Void> fn = l -> {
                consumer.accept(l);
                return null;
            };
            vfl.callSecondaryJoiningBlock(blockName, startMessage, fn, null);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return CompletableFuture.runAsync(() -> {
            Function<VFLFn, Void> fn = l -> {
                consumer.accept(l);
                return null;
            };
            vfl.callSecondaryNonJoiningBlock(blockName, startMessage, fn::apply);
        }, executor);
    }
}
