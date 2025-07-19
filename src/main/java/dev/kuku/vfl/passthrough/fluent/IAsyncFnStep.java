package dev.kuku.vfl.passthrough.fluent;

import dev.kuku.vfl.passthrough.IPassthroughVFL;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IAsyncFnStep {
    IAsyncFnStep withExecutor(Executor executor);

    <R> ISubBlockCallStep<R> andCall(Function<IPassthroughVFL, R> callable);

    CompletableFuture<Void> run(Consumer<IPassthroughVFL> toRun);
}
