package dev.kuku.vfl.passthrough.fluent;

import dev.kuku.vfl.passthrough.IPassthroughVFL;

import java.util.function.Function;

public class SubBlockCallStep<R> implements ISubBlockCallStep<R> {
    private final Function<IPassthroughVFL, R> fn;
    private final IPassthroughVFL logger;
    private final String blockName;
    private final String msg;
    private Function<R, String> endMsgFn = null;

    public SubBlockCallStep(Function<IPassthroughVFL, R> fn, IPassthroughVFL logger, String blockName, String msg) {
        this.fn = fn;
        this.logger = logger;
        this.blockName = blockName;
        this.msg = msg;
    }

    @Override
    public ISubBlockCallStep<R> withEndMsg(Function<R, String> endMsgFn) {
        this.endMsgFn = endMsgFn;
        return this;
    }

    @Override
    public R call() {
        return logger.call(blockName, msg, endMsgFn, fn);
    }
}
