package dev.kuku.vfl.passthrough.fluent;

import dev.kuku.vfl.passthrough.IPassthroughVFL;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ISubBlockStartStep {
    ISubBlockStartStep withMsg(String msg);

    ISubBlockRunStep andRun(Consumer<IPassthroughVFL> runnable);

    <R> ISubBlockCallStep<R> andCall(Function<IPassthroughVFL, R> callable);


}