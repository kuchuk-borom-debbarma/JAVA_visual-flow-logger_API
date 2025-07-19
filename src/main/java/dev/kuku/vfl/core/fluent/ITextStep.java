package dev.kuku.vfl.core.fluent;

import java.util.concurrent.Callable;

public interface ITextStep {
    <R> IFnTextStep<R> fn(Callable<R> callable);

    void msg(String msg);

    void error(String error);

    void warn(String warn);
}