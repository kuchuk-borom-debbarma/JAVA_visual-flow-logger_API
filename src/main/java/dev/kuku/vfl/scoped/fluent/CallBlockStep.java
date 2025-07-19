package dev.kuku.vfl.scoped.fluent;

import dev.kuku.vfl.core.util.HelperUtil;
import dev.kuku.vfl.scoped.ScopedVFL;

import java.util.concurrent.Callable;
import java.util.function.Function;

class CallBlockStep<R> implements ICallBlockStep<R> {
    private final Callable<R> callable;
    private String blockName;
    private String startBlockMsg;
    private Function<R, String> endMsgFn = null;

    CallBlockStep(Callable<R> callable) {
        this.callable = callable;
    }

    @Override
    public R call() {
        String bn = blockName;
        if (bn == null) {
            bn = HelperUtil.getLambdaOriginMethodName();
        }
        return ScopedVFL.get().call(bn, startBlockMsg, endMsgFn, callable);
    }

    @Override
    public ICallBlockStep<R> endMsg(Function<R, String> endMsgFn) {
        this.endMsgFn = endMsgFn;
        return this;
    }

    @Override
    public ICallBlockStep<R> withBlockName(String blockName) {
        this.blockName = blockName;
        return this;
    }

    @Override
    public ICallBlockStep<R> withMsg(String message) {
        this.startBlockMsg = message;
        return null;
    }
}
