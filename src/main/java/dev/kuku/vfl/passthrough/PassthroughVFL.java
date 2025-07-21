package dev.kuku.vfl.passthrough;

import dev.kuku.vfl.StartBlockHelper;
import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LoggerAndBlockLogData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

class PassthroughVFL extends VFL implements IPassthroughVFL {

    PassthroughVFL(VFLBlockContext context) {
        super(context);
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        BlockData bd = new BlockData(id, blockContext.blockInfo.getId(), blockName);
        blockContext.buffer.pushBlockToBuffer(bd);
        return bd;
    }

    @Override
    public void run(String blockName, String message, Consumer<IPassthroughVFL> fn) {
        ensureBlockStarted();
        StartBlockHelper.SetupStartBlock(blockName, message, true, blockContext, PassthroughVFL::new, loggerAndBlockLogData -> StartBlockHelper.RunFnForLogger(() -> fn.accept((IPassthroughVFL) loggerAndBlockLogData.logger()), null, loggerAndBlockLogData.logger()));
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn, Executor executor) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = StartBlockHelper.SetupStartBlock(blockName, message, false, blockContext, PassthroughVFL::new, null);
        return CompletableFuture.runAsync(() -> StartBlockHelper.RunFnForLogger(() -> fn.accept((IPassthroughVFL) setupResult.logger()), null, setupResult.logger()), executor);
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = StartBlockHelper.SetupStartBlock(blockName, message, false, blockContext, PassthroughVFL::new, null);
        return CompletableFuture.runAsync(() -> StartBlockHelper.RunFnForLogger(() -> fn.accept((IPassthroughVFL) setupResult.logger()), null, setupResult.logger()));
    }

    @Override
    public <R> R call(String blockName, String message, Function<R, String> endMessageFn,
                      Function<IPassthroughVFL, R> fn) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = StartBlockHelper.SetupStartBlock(blockName, message, true, blockContext, PassthroughVFL::new, null);
        return StartBlockHelper.CallFnForLogger(() -> fn.apply((PassthroughVFL) setupResult.logger()), endMessageFn, null, setupResult.logger());
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Function<R, String> endMessageFn,
                                              Function<IPassthroughVFL, R> fn, Executor executor) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = StartBlockHelper.SetupStartBlock(blockName, message, false, blockContext, PassthroughVFL::new, null);
        return CompletableFuture.supplyAsync(() -> StartBlockHelper.CallFnForLogger(() -> fn.apply((IPassthroughVFL) setupResult.logger()), endMessageFn, null, setupResult.logger()), executor);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn) {
        ensureBlockStarted();
        LoggerAndBlockLogData setupResult = StartBlockHelper.SetupStartBlock(blockName, message, false, blockContext, PassthroughVFL::new, null);
        return CompletableFuture.supplyAsync(() -> StartBlockHelper.CallFnForLogger(() -> fn.apply((IPassthroughVFL) setupResult.logger()), endMessageFn, null, setupResult.logger()));
    }

}
