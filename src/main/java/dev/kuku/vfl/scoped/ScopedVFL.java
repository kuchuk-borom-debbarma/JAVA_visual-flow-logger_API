package dev.kuku.vfl.scoped;

import dev.kuku.vfl.BlockHelper;
import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.models.LoggerAndBlockLogData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public class ScopedVFL extends VFL implements IScopedVFL {

    /// Holds instance per scope that attempts to access it.
    public static final ScopedValue<IScopedVFL> scopedInstance =
            ScopedValue.newInstance();

    ScopedVFL(VFLBlockContext context) {
        super(context);
    }

    public static IScopedVFL get() {
        if (!ScopedVFL.scopedInstance.isBound()) {
            throw new IllegalStateException(
                    "scopedBlockData is not within ScopedValue bound. Please use " +
                            ScopedVFLRunner.class.getName() +
                            " to start a new scope."
            );
        }
        return ScopedVFL.scopedInstance.get();
    }

    @Override
    public void run(String blockName, String blockMessage, Runnable runnable) {
        ensureBlockStarted();
        BlockHelper.SetupStartBlock(blockName, blockMessage, true, blockContext, ScopedVFL::new,
                loggerAndBlockLogData -> {
                    //Create a new scoped logger with the sub logger as value and run the passed runnable in that scope
                    ScopedValue.where(scopedInstance, (ScopedVFL) loggerAndBlockLogData.logger())
                            .run(() -> BlockHelper.RunFnForLogger(runnable, null, loggerAndBlockLogData.logger()));
                });
    }

    @Override
    public CompletableFuture<Void> runAsync(
            String blockName,
            String blockMessage,
            Runnable runnable,
            Executor executor
    ) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = BlockHelper.SetupStartBlock(blockName, blockMessage, false, blockContext, ScopedVFL::new, null);
        return CompletableFuture.runAsync(() -> ScopedValue.where(scopedInstance, (ScopedVFL) setupResult.logger())
                .run(() -> BlockHelper.RunFnForLogger(runnable, null, setupResult.logger())), executor);
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = BlockHelper.SetupStartBlock(blockName, blockMessage, false, blockContext, ScopedVFL::new, null);
        return CompletableFuture.runAsync(() -> ScopedValue.where(scopedInstance, (ScopedVFL) setupResult.logger())
                .run(() -> BlockHelper.RunFnForLogger(runnable, null, setupResult.logger())));
    }

    @Override
    public <R> R call(
            String blockName,
            String blockMessage,
            Function<R, String> endMessageFn,
            Callable<R> callable
    ) {
        ensureBlockStarted();
        LoggerAndBlockLogData result = BlockHelper.SetupStartBlock(blockName, blockMessage, true, blockContext, ScopedVFL::new, null);
        //Create a new scope and set it's logger value to sub logger from result and call the passed callable in that scope
        return ScopedValue.where(scopedInstance, (ScopedVFL) result.logger())
                .call(() -> BlockHelper.CallFnForLogger(callable, endMessageFn, null, result.logger()));
    }

    @Override
    public <R> CompletableFuture<R> callAsync(
            String blockName,
            String blockMessage,
            Function<R, String> endMessageFn,
            Callable<R> callable,
            Executor executor
    ) {
        ensureBlockStarted();
        LoggerAndBlockLogData result = BlockHelper.SetupStartBlock(blockName, blockMessage, false, blockContext, ScopedVFL::new, null);
        return CompletableFuture.supplyAsync(() ->
                ScopedValue.where(scopedInstance, (ScopedVFL) result.logger())
                        .call(() -> BlockHelper.CallFnForLogger(callable, endMessageFn, null, result.logger())), executor);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable) {
        ensureBlockStarted();
        LoggerAndBlockLogData result = BlockHelper.SetupStartBlock(blockName, blockMessage, false, blockContext, ScopedVFL::new, null);
        return CompletableFuture.supplyAsync(() ->
                ScopedValue.where(scopedInstance, (ScopedVFL) result.logger())
                        .call(() -> BlockHelper.CallFnForLogger(callable, endMessageFn, null, result.logger())));
    }
}
