package dev.kuku.vfl.passthrough.fluent;

import java.util.function.Function;

public interface ISubBlockCallStep<R> {
    ISubBlockCallStep<R> withEndMsg(Function<R, String> endMsgFn);

    R call();
}