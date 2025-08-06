package dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps;

import dev.kuku.vfl.core.fluent.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.sub_block.AsSubBlockSupplierAsyncStep;

import java.util.function.Supplier;

public class ThreadVFLSupplierAsyncStep<R> extends SupplierStep<R> {
    public ThreadVFLSupplierAsyncStep(VFL vfl, Supplier<R> supplier) {
        super(vfl, supplier);
    }

    public AsSubBlockSupplierAsyncStep<R> asSubBlock(String blockName) {
        return new AsSubBlockSupplierAsyncStep<>(supplier, blockName);
    }
}