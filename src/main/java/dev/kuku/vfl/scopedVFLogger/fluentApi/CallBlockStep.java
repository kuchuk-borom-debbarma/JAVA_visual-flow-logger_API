package dev.kuku.vfl.scopedVFLogger.fluentApi;

import java.util.function.Function;

public interface CallBlockStep<R> {
    R call();

    CallBlockStep<R> endMsg(Function<R, String> endMsgFn);

    CallBlockStep<R> withBlockName(String blockName);

    CallBlockStep<R> withMsg(String message);
}
