package dev.kuku.vfl.impl.threadlocal.flient_steps;

import dev.kuku.vfl.core.fluent_api.base.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block.AsSubBlockCallableStep;

import java.util.function.Supplier;

public class CallableSupplierStep<R> extends SupplierStep<R> {
    public CallableSupplierStep(VFL vfl, Supplier<R> supplier) {
        super(vfl, supplier);
    }

    public AsSubBlockCallableStep<R> asSubBlock(String name) {
        return new AsSubBlockCallableStep<>(supplier, name);
    }
}
