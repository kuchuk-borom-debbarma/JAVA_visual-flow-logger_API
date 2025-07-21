package dev.kuku.vfl.passthrough;

import dev.kuku.vfl.StartBlockHelper;
import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.models.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

class PassthroughVFL extends VFL implements IPassthroughVFL {

    PassthroughVFL(VFLBlockContext context) {
        super(context);
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        BlockData bd = new BlockData(id, blockContext.blockInfo.getId(), blockName);
        blockContext.buffer.pushBlockToBuffer(bd);
        return bd;
    }
    //TODO create VFLWithBlock abstract class which will extend VFL and give block start related functions and moe StarBlokHelper too
    private LoggerAndBlockLogData preSubBlockFn(String blockName, String blockMessage, boolean move) {
        ensureBlockStarted();
        String subBlockId = generateUID();
        BlockData subBlockData = createAndPushBlockData(subBlockId, blockName);
        LogData subBlockStartLog = createLogAndPush(VflLogType.SUB_BLOCK_START, blockMessage, subBlockId);
        IPassthroughVFL subBlockLogger = new PassthroughVFL(new VFLBlockContext(subBlockData, blockContext.buffer));
        if (move) {
            blockContext.currentLogId = subBlockStartLog.getId();
        }
        return new LoggerAndBlockLogData(subBlockLogger, subBlockData, subBlockStartLog);
    }

    private <R> R fnHandler(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn, boolean move) {
        var result = preSubBlockFn(blockName, message, move);
        return StartBlockHelper.callFnForLogger(() -> fn.apply((IPassthroughVFL) result.logger()), endMessageFn, null, result.logger());
    }

    private <R> CompletableFuture<R> asyncFnHandler(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn, Executor executor) {
        return CompletableFuture.supplyAsync(() -> fnHandler(blockName, message, endMessageFn, fn, false), executor);
    }

    @Override
    public void run(String blockName, String message, Consumer<IPassthroughVFL> fn) {
        fnHandler(blockName, message, null, (l) -> {
            fn.accept(l);
            return null;
        }, true);
    }

    @Override
    public CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn, Executor executor) {
        return asyncFnHandler(blockName, message, null, (l) -> {
            fn.accept(l);
            return null;
        }, executor);
    }

    @Override
    public <R> R call(String blockName, String message, Function<R, String> endMessageFn,
                      Function<IPassthroughVFL, R> fn) {
        return fnHandler(blockName, message, endMessageFn, fn, true);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Function<R, String> endMessageFn,
                                              Function<IPassthroughVFL, R> fn, Executor executor) {
        return asyncFnHandler(blockName, message, endMessageFn, fn, executor);
    }

}
