package dev.kuku.vfl.core.vfl_abstracts;

import java.util.concurrent.Callable;
import java.util.function.Function;

public abstract class VFLCallable extends VFL {
    public <R> R callPrimarySubBlock(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        ensureBlockStarted();
    }

    public <R> R callNonJoiningSecondaryBlock(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        ensureBlockStarted();
    }

    public <R> R callJoiningSecondaryBlock(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        ensureBlockStarted();
    }
}