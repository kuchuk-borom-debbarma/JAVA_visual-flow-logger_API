package dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.ThreadVFL;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.UpdateEndMsg;

@RequiredArgsConstructor
public class AsSubBlockSupplierAsyncStep<R> {
    private final Supplier<R> fn;
    private final String blockName;
    private String startMessage;
    private Function<R, String> endMessage;
    private Executor executor;


    public AsSubBlockSupplierAsyncStep<R> withStartMessage(String startMessage) {
        this.startMessage = startMessage;
        return this;
    }

    public AsSubBlockSupplierAsyncStep<R> withEndMessage(Function<R, String> endMessage, Object... args) {
        this.endMessage = UpdateEndMsg(endMessage, args);
        return this;
    }

    public AsSubBlockSupplierAsyncStep<R> withEndMessage(String endMessage, Object... args) {
        this.endMessage = (r) -> {
            Object[] a = Util.combineArgsWithReturn(args, r);
            return Util.FormatMessage(endMessage, a);
        };
        return this;
    }

    public AsSubBlockSupplierAsyncStep<R> withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public CompletableFuture<R> executeDetached() {
        if (executor != null) {
            return ThreadVFL.getCurrentLogger().supplyAsyncWith(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, endMessage, executor);
        }
        return ThreadVFL.getCurrentLogger().supplyAsync(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, endMessage);
    }

    public CompletableFuture<R> executeFork() {
        if (executor != null) {
            return ThreadVFL.getCurrentLogger().supplyAsyncWith(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, endMessage, executor);
        }
        return ThreadVFL.getCurrentLogger().supplyAsync(blockName, startMessage, fn, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, endMessage);
    }
}