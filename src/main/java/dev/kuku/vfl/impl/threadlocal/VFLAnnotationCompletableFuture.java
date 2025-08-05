package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static dev.kuku.vfl.impl.threadlocal.ThreadVFLAnnotation.buffer;

public class VFLAnnotationCompletableFuture {

    public static <R> CompletableFuture<R> supplyAsync(String blockName, Supplier<R> supplier) {
        ThreadVFL callerLogger = ThreadVFL.getCurrentLogger();
        VFLBlockContext parentBlock = callerLogger.loggerContext;
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, parentBlock.blockInfo.getId(), buffer);
        SubBlockStartLog subBlockStartLog = VFLHelper.CreateLogAndPush2Buffer(
                parentBlock.blockInfo.getId(),
                parentBlock.currentLogId,
                null,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN, buffer);
        //Since it's a secondary sub block start, this will not move the flow
        //parentBlock.currentLogId = subBlockStartLog.getId();
        return CompletableFuture.supplyAsync((() -> {
            ThreadVFLAnnotation.startedSubBlockInParentThread.set(subBlock);
            return supplier.get();
        }));
    }

    public static CompletableFuture<Void> runAsync(String blockName, Runnable runnable) {
        ThreadVFL callerLogger = ThreadVFL.getCurrentLogger();
        VFLBlockContext parentBlock = callerLogger.loggerContext;
        Block subBlock = VFLHelper.CreateBlockAndPush2Buffer(blockName, parentBlock.blockInfo.getId(), buffer);
        SubBlockStartLog subBlockStartLog = VFLHelper.CreateLogAndPush2Buffer(
                parentBlock.blockInfo.getId(),
                parentBlock.currentLogId,
                null,
                subBlock.getId(),
                LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN, buffer);
        //Since it's a secondary sub block start, this will not move the flow
        //parentBlock.currentLogId = subBlockStartLog.getId();
        return CompletableFuture.runAsync((() -> {
            ThreadVFLAnnotation.startedSubBlockInParentThread.set(subBlock);
            runnable.run();
        }));
    }
}
