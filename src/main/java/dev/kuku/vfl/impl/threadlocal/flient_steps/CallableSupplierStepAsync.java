package dev.kuku.vfl.impl.threadlocal.flient_steps;

import dev.kuku.vfl.core.fluent_api.base.steps.SupplierStep;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

import java.util.function.Supplier;

public class CallableSupplierStepAsync<R> extends SupplierStep<R> {
    public CallableSupplierStepAsync(VFL vfl, Supplier<R> supplier) {
        super(vfl, supplier);
    }
}
