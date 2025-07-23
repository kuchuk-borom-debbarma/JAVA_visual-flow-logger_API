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


    /*
     * TODO branch for fire and forget or event
     * The idea is to add a sub_block_start log to the sequence and return the logger
     * Then we can pass the branch logger to the event publisher. Event listeners will create sub block starts * of its own without moving the chain with secondary = true.
     *
     * For scenario where its a fire and forget operation which takes in a function. Create a secondary non joining back sub block start.
     *
     * TODO :- Define complex log types clearly 
     */
}
