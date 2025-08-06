package dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps;

import dev.kuku.vfl.core.fluent.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.impl.threadlocal_annotation.fluent.flient_steps.sub_block.AsSubBlockCallableStep;

import java.util.function.Supplier;

public class ThreadVFLSupplierStep<R> extends SupplierStep<R> {
    public ThreadVFLSupplierStep(VFL vfl, Supplier<R> supplier) {
        super(vfl, supplier);
    }

    public AsSubBlockCallableStep<R> asSubBlock(String name) {
        return new AsSubBlockCallableStep<>(supplier, name);
    }
}
