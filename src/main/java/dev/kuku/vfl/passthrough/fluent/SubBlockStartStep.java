package dev.kuku.vfl.passthrough.fluent;

import dev.kuku.vfl.passthrough.IPassthroughVFL;

import java.util.function.Consumer;
import java.util.function.Function;

class SubBlockStartStep implements ISubBlockStartStep {
    private final IPassthroughVFL loggger;
    private String msg = null;

    private final String blockName;

    SubBlockStartStep(IPassthroughVFL loggger, String blockName) {
        this.loggger = loggger;
        this.blockName = blockName;
    }

    @Override
    public ISubBlockStartStep withMsg(String msg) {
        this.msg = msg;
        return this;
    }

    @Override
    public ISubBlockRunStep andRun(Consumer<IPassthroughVFL> runnable) {
        return new SubBlockRunStep();
    }

    @Override
    public <R> ISubBlockCallStep<R> andCall(Function<IPassthroughVFL, R> callable) {
        return new SubBlockCallStep<R>(callable, loggger, blockName, msg);
    }


}