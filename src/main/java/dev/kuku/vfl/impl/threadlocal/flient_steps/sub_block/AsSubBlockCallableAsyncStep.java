package dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block;

import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.ThreadVFL;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;
import static dev.kuku.vfl.core.helpers.VFLHelper.UpdateEndMsg;

@RequiredArgsConstructor
public class AsSubBlockCallableAsyncStep<R> {
    private final Supplier<R> supplier;
    private final String blockName;
    private String startMessage;
    private Function<R, String> endMessage;
    private Executor executor;


    public AsSubBlockCallableAsyncStep<R> withStartMessage(String startMessage) {
        this.startMessage = startMessage;
        return this;
    }

    public AsSubBlockCallableAsyncStep<R> withEndMessage(Function<R, String> endMessage, Object... args) {
        this.endMessage = UpdateEndMsg(endMessage, args);
        return this;
    }

    public AsSubBlockCallableAsyncStep<R> withEndMessage(String endMessage, Object... args) {
        this.endMessage = (r) -> FormatMessage(endMessage, args, r);
        return this;
    }

    public AsSubBlockCallableAsyncStep<R> withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public CompletableFuture<R> executeDetached() {
        if (executor != null) {
            return ThreadVFL.getCurrentLogger().supplyAsyncWith(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, endMessage, executor);
        }
        return ThreadVFL.getCurrentLogger().supplyAsync(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, endMessage);
    }

    public CompletableFuture<R> executeFork() {
        if (executor != null) {
            return ThreadVFL.getCurrentLogger().supplyAsyncWith(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, endMessage, executor);
        }
        return ThreadVFL.getCurrentLogger().supplyAsync(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, endMessage);
    }
}
