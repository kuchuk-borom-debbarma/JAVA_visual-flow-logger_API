package dev.kuku.vfl.core.fluent_api.callable.steps;

import dev.kuku.vfl.core.fluent_api.base.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.function.Supplier;

public class CallableSupplierStep<R> extends SupplierStep<R> {
    private final VFLCallable vfl;

    public CallableSupplierStep(VFLCallable vfl, Supplier supplier) {
        super(vfl, supplier);
        this.vfl = vfl;
    }

    public AsSubBlockStep asSubBlock(String blockName) {
        return new AsSubBlockStep(blockName, vfl, supplier);
    }
}
