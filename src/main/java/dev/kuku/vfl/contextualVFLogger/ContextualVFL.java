package dev.kuku.vfl.contextualVFLogger;

import dev.kuku.vfl.core.VFL;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ContextualVFL extends VFL {
    void run(String blockName, String message, Consumer<ContextualVFL> runnable);

    void runHere(String blockName, String message, Consumer<ContextualVFL> runnable);

    <R> R call(String blockName, String message, Function<R, String> endMessageFn, Function<ContextualVFL, R> callable);

    <R> R call(String blockName, String message, Function<ContextualVFL, R> callable);

    <R> R callHere(String blockName, String message, Function<R, String> endMessageFn, Function<ContextualVFL, R> callable);

    <R> R callHere(String blockName, String message, Function<ContextualVFL, R> callable);
}
