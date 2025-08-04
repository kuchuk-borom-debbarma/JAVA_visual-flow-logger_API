package dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.ThreadVFL;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class AsSubBlockSupplierAsyncStep<R> {
    private final Supplier<R> fn;
    private final String blockName;
    private String startMessage;
    private Function<R, String> endMessage;
    private Executor executor;

    private Function<R, String> updateEndMsg(Function<R, String> msgSerializer, Object... args) {
        return (r) -> {
            // Get the message template from the user's serializer
            String messageTemplate = msgSerializer.apply(r);

            // Format the message with user args + return value
            // Args convention: user args fill {0}, {1}, {2}... and return value fills the last placeholder
            return Util.FormatMessage(messageTemplate, args, r);
        };
    }

    public AsSubBlockSupplierAsyncStep<R> withStartMessage(String startMessage) {
        this.startMessage = startMessage;
        return this;
    }

    public AsSubBlockSupplierAsyncStep<R> withEndMessage(Function<R, String> endMessage, Object... args) {
        this.endMessage = updateEndMsg(endMessage, args);
        return this;
    }

    public AsSubBlockSupplierAsyncStep<R> withEndMessage(String endMessage, Object... args) {
        this.endMessage = (r) -> Util.FormatMessage(endMessage, args, r);
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