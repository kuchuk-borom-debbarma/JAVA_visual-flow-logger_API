package dev.kuku.vfl.threadLocal;

import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public interface IThreadLocal extends IVFL {
    <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor);

    <R> R call(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn);

    class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> call) {
            try {
                return ThreadLocaVFL.start(blockName, buffer, call);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}