package dev.kuku.vfl.core.fluent_api.subBlockCommons;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface AsyncBlockExecutor<R> {
    CompletableFuture<R> startSecondaryJoining(Executor executor);

    CompletableFuture<Void> startSecondaryNonJoining(Executor executor);
}