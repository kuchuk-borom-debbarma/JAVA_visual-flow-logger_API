package dev.kuku.vfl.core.fluent_api.callable.steps;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockCallableEndMessage;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockStartMsg;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.StartSubBlockStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class AsSubBlockStep<R> implements BlockStartMsg, BlockCallableEndMessage, StartSubBlockStep<R> {
    private final String blockName;
    private final VFLCallable vfl;
    private final Supplier<R> supplier;
    private Function<R, String> endMessage = null;
    private String startMessage = null;

    @Override
    public BlockCallableEndMessage withEndMessageMapper(Function endMessageSerializer) {
        this.endMessage = endMessageSerializer;
        return this;
    }

    @Override
    public BlockStartMsg withStartMessage(String startMessage) {
        this.startMessage = startMessage;
        return this;
    }

    @Override
    public R startPrimary() {
        return vfl.callPrimarySubBlock(blockName, startMessage, supplier, endMessage);
    }

    @Override
    public CompletableFuture<R> startSecondaryJoining(Executor executor) {
        return vfl.callSecondaryJoiningBlock(blockName, startMessage, supplier, endMessage, executor);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return vfl.callSecondaryNonJoiningBlock(blockName, startMessage, () -> supplier.get(), executor);
    }
}
