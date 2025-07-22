package dev.kuku.vfl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IPassthroughVFL extends IVFL {

    void run(String blockName, String message, Consumer<IPassthroughVFL> fn);

    CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn, Executor executor);

    CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn);

    <R> R call(String blockName, String message, Function<IPassthroughVFL, R> fn, Function<R, String> endMessageFn);

    <R> CompletableFuture<R> callAsync(String blockName, String message,
                                       Function<IPassthroughVFL, R> fn, Function<R, String> endMessageFn, Executor executor);

    <R> CompletableFuture<R> callAsync(String blockName, String message,
                                       Function<IPassthroughVFL, R> fn, Function<R, String> endMessageFn);
}
