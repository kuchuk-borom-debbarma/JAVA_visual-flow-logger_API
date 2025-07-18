package dev.kuku.vfl.scopedVFLogger.fluentApi;

import dev.kuku.vfl.core.util.HelperUtil;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;

import java.util.concurrent.Callable;
import java.util.function.Function;

class CallBlockStepImpl<R> implements CallBlockStep<R> {
    private final Callable<R> callable;
    private String blockName;
    private String startBlockMsg;
    private Function<R, String> endMsgFn = null;

    CallBlockStepImpl(Callable<R> callable) {
        this.callable = callable;
    }

    @Override
    public R call() {
        String bn = blockName;
        if (bn == null) {
            bn = HelperUtil.getLambdaOriginMethodName();
        }
        return ScopedVFLImpl.get().call(bn, startBlockMsg, endMsgFn, callable);
    }

    @Override
    public CallBlockStep<R> endMsg(Function<R, String> endMsgFn) {
        this.endMsgFn = endMsgFn;
        return this;
    }

    @Override
    public CallBlockStep<R> withBlockName(String blockName) {
        this.blockName = blockName;
        return this;
    }

    @Override
    public CallBlockStep<R> withMsg(String message) {
        this.startBlockMsg = message;
        return null;
    }
}
