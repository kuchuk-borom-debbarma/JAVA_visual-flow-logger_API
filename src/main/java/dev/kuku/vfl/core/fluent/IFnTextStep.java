package dev.kuku.vfl.core.fluent;

import java.util.function.Function;

public interface IFnTextStep<R> {
    ITextFnStep<R> textFn(Function<R, String> fn);
}