package dev.kuku.vfl.core.fluent_api.callable;

import dev.kuku.vfl.core.fluent_api.base.FluentVFL;
import dev.kuku.vfl.core.fluent_api.callable.steps.CallableSupplierStep;
import dev.kuku.vfl.core.fluent_api.callable.steps.runnable.RunSubBlockStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.function.Supplier;

public class FluentVFLCallable extends FluentVFL {

    public FluentVFLCallable(VFLCallable logger) {
        super(logger);
    }

    @Override
    public <R> CallableSupplierStep<R> call(Supplier<R> fn) {
        return new CallableSupplierStep<>((VFLCallable) vfl, fn);
    }

    public RunSubBlockStep runSubBlock(Runnable r) {
        return new RunSubBlockStep((VFLCallable) vfl, r);
    }
}
