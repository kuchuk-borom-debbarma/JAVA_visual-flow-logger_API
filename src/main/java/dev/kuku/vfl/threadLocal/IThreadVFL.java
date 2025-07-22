package dev.kuku.vfl.threadLocal;

import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public interface IThreadVFL extends IVFL {

    <R> void run(String blockName, String startMessage, Runnable runnable);

    <R> CompletableFuture<Void> runAsync(String blockName, String startMessage, Runnable runnable, Executor executor);

    <R> CompletableFuture<Void> runAsync(String blockName, String startMessage, Runnable runnable);

    <R> R call(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMsgFn);

    <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor);

    <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn);


    class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> call) {
            try {
                return ThreadVFL.start(blockName, buffer, call);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}