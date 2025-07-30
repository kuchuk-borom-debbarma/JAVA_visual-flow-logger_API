package dev.kuku.vfl.core.fluent_api.callable.steps.runnable;

import dev.kuku.vfl.core.fluent_api.subBlockCommons.AsyncBlockExecutor;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockExecutor;
import dev.kuku.vfl.core.fluent_api.subBlockCommons.BlockStartMsg;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static dev.kuku.vfl.core.helpers.Util.FormatMessage;

@RequiredArgsConstructor
public class RunBlockWithNameStep implements BlockStartMsg, BlockExecutor<Void>, AsyncBlockExecutor<Void> {

    private final String blockName;
    private final VFLCallable vflCallable;
    private final Runnable runnable;
    private String startMessage = null;

    @Override
    public RunBlockWithNameStep withStartMessage(String startMessage, Object... args) {
        this.startMessage = FormatMessage(startMessage, args);
        return this;
    }

    @Override
    public Void startPrimary() {
        return vflCallable.callPrimarySubBlock(blockName, startMessage, () -> {
            runnable.run();
            return null;
        }, null);
    }

    @Override
    public CompletableFuture<Void> startSecondaryJoining(Executor executor) {
        return vflCallable.callSecondaryJoiningBlock(blockName, startMessage, () -> {
            runnable.run();
            return null;
        }, executor, null);
    }

    @Override
    public CompletableFuture<Void> startSecondaryNonJoining(Executor executor) {
        return vflCallable.callSecondaryNonJoiningBlock(blockName, startMessage, runnable::run, executor);
    }
}
