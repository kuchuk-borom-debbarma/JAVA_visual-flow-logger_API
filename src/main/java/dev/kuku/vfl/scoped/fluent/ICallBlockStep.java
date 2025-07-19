package dev.kuku.vfl.scoped.fluent;

import java.util.function.Function;

public interface ICallBlockStep<R> {
    R call();

    ICallBlockStep<R> endMsg(Function<R, String> endMsgFn);

    ICallBlockStep<R> withBlockName(String blockName);

    ICallBlockStep<R> withMsg(String message);
}
