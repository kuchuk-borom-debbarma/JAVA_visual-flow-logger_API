package dev.kuku.vfl.impl.threadlocal.fluent.flient_steps;

import dev.kuku.vfl.core.fluent_api.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal.fluent.flient_steps.sub_block.AsSubBlockSupplierAsyncStep;

import java.util.function.Supplier;

public class ThreadVFLSupplierAsyncStep<R> extends SupplierStep<R> {
    public ThreadVFLSupplierAsyncStep(VFL vfl, Supplier<R> supplier) {
        super(vfl, supplier);
    }

    public AsSubBlockSupplierAsyncStep<R> asSubBlock(String blockName) {
        return new AsSubBlockSupplierAsyncStep<>(supplier, blockName);
    }
}