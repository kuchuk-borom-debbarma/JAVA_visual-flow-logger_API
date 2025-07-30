package dev.kuku.vfl.core.fluent_api.callable.steps;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.AsyncBlockExecutor;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockCallableEndMessage;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockExecutor;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockStartMsg;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

@RequiredArgsConstructor
public class AsSubBlockStep<R> implements BlockStartMsg, BlockCallableEndMessage<R>, BlockExecutor<R>, AsyncBlockExecutor<R> {
    private final String blockName;
    private final VFLCallable vfl;
    private final Supplier<R> supplier;
    private Function<R, String> endMessage = null;
    private String startMessage = null;
    private Object[] endArgs;

    @Override
    public AsSubBlockStep<R> withEndMessageMapper(Function<R, String> endMessageSerializer, Object... args) {
        this.endMessage = endMessageSerializer;
        this.endArgs = args;
        return this;
    }

    @Override
    public AsSubBlockStep<R> withStartMessage(String startMessage, Object... args) {
        this.startMessage = FormatMessage(startMessage, args);
        return this;
    }

    @Override
    public R startPrimary() {
        return vfl.callPrimarySubBlock(blockName, startMessage, supplier, endMessage, endArgs);
    }

    @Override
    public CompletableFuture<R> startSecondaryJoining(Executor executor) {
        return vfl.callSecondaryJoiningBlock(blockName, startMessage, supplier, executor, endMessage, endArgs);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return vfl.callSecondaryNonJoiningBlock(blockName, startMessage, supplier::get, executor);
    }
}
