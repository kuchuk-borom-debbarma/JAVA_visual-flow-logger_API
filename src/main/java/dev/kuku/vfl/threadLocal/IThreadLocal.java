package dev.kuku.vfl.threadLocal;

import dev.kuku.vfl.core.IVFL;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

public interface IThreadLocal extends IVFL {
    <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor);

    <R> R call(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor);
}