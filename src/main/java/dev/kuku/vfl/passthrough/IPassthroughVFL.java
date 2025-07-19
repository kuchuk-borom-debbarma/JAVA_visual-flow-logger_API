package dev.kuku.vfl.passthrough;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.kuku.vfl.core.IVFL;

public interface IPassthroughVFL extends IVFL {

    void run(String blockName, String message, Consumer<IPassthroughVFL> fn);

    CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn, Executor executor);

    <R> R call(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn);

    <R> CompletableFuture<R> callAsync(String blockName, String message, Function<R, String> endMessageFn,
            Function<IPassthroughVFL, R> fn, Executor executor);

    class Runner {
    }
}
//TODO runner and flunt api