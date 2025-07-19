package dev.kuku.vfl.core.fluent;

import java.util.concurrent.Callable;
import java.util.function.Function;

public interface IFnTextStep<R> {
    ITextFnStep textFn(Function<R, String> fn);
}