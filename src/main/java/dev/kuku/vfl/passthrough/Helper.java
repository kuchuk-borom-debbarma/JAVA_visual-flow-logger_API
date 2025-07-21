package dev.kuku.vfl.passthrough;

import dev.kuku.vfl.StartBlockHelper;

import java.util.function.Function;

public class Helper {

    public static <R> R blockFnLifeCycleHandler(Function<IPassthroughVFL, R> fn, Function<R, String> endMessageFn, IPassthroughVFL subBlockLogger) {
        return StartBlockHelper.ProcessCallableForLogger(() -> fn.apply(subBlockLogger), endMessageFn, null, subBlockLogger);
    }
}
