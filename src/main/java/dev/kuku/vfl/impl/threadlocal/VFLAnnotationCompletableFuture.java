package dev.kuku.vfl.impl.threadlocal;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
import dev.kuku.vfl.impl.threadlocal.dto.SubBlockStartExecutorData;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VFLAnnotationCompletableFuture {

    public static <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        ThreadVFL callerLogger = ThreadVFL.getCurrentLogger();
        VFLBlockContext parentBlock = callerLogger.loggerContext;
        SubBlockStartExecutorData spawnedThreadData = new SubBlockStartExecutorData(parentBlock, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_JOIN);
        return CompletableFuture.supplyAsync((() -> {
            ThreadVFLAnnotation.parentThreadLoggerData.set(spawnedThreadData);
            return supplier.get();
        }));
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        ThreadVFL callerLogger = ThreadVFL.getCurrentLogger();
        VFLBlockContext parentBlock = callerLogger.loggerContext;
        SubBlockStartExecutorData spawnedThreadData = new SubBlockStartExecutorData(parentBlock, LogTypeBlockStartEnum.SUB_BLOCK_START_SECONDARY_NO_JOIN);
        return CompletableFuture.runAsync((() -> {
            ThreadVFLAnnotation.parentThreadLoggerData.set(spawnedThreadData);
            runnable.run();
        }));
    }
}
