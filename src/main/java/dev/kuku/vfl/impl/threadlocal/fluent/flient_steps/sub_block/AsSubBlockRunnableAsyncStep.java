package dev.kuku.vfl.impl.threadlocal.fluent.flient_steps.sub_block;

import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.logger.ThreadVFL;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

@RequiredArgsConstructor
public class AsSubBlockRunnableAsyncStep {
    private final Runnable supplier;
    private final String blockName;
    private String startMessage;
    private Executor executor;


    public AsSubBlockRunnableAsyncStep withStartMessage(String startMessage, Object... args) {
        this.startMessage = FormatMessage(startMessage, args);
        return this;
    }


    public AsSubBlockRunnableAsyncStep withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public CompletableFuture<Void> executeDetached() {
        if (executor != null) {
            return ThreadVFL.getCurrentLogger().runAsyncWith(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, executor);
        }
        return ThreadVFL.getCurrentLogger().runAsync(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN);
    }

    public CompletableFuture<Void> executeFork() {
        if (executor != null) {
            return ThreadVFL.getCurrentLogger().runAsyncWith(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, executor);
        }
        return ThreadVFL.getCurrentLogger().runAsync(blockName, startMessage, supplier, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN);
    }
}
