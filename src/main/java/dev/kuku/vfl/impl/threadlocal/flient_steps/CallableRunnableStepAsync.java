package dev.kuku.vfl.impl.threadlocal.flient_steps;

import dev.kuku.vfl.core.fluent_api.base.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal.flient_steps.sub_block.AsSubBlockCallableAsyncStep;

import java.util.function.Supplier;

public class CallableRunnableStepAsync<R> extends SupplierStep<R> {

    public CallableRunnableStepAsync(VFL vfl, Supplier<R> supplier) {
        super(vfl, supplier);
    }

    public AsSubBlockCallableAsyncStep<R> asSubBlock(String blockName) {
        return new AsSubBlockCallableAsyncStep<>(supplier, blockName);
    }
}
