package dev.kuku.vfl.core.abstracts;

import dev.kuku.vfl.core.helpers.VFLHelper;

import java.util.concurrent.Callable;
import java.util.function.Function;

public abstract class VFLCallable extends VFL {

    abstract VFL getLogger();

    public <R> R call(String blockName, String startMessage, Callable<R> callable, Function<R, String> endMessageSerializer) {
        return VFLHelper.CallFnWithLogger(callable, getLogger(), endMessageSerializer);
    }
}