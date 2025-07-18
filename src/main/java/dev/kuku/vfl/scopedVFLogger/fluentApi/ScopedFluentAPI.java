package dev.kuku.vfl.scopedVFLogger.fluentApi;

import dev.kuku.vfl.core.util.HelperUtil;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;
import dev.kuku.vfl.scopedVFLogger.fluentApi.subBlockStep.CallStep;
import dev.kuku.vfl.scopedVFLogger.fluentApi.subBlockStep.SubBlockStep;

public class ScopedFluentAPI implements ScopeFluentVFL, SubBlockStep {
    //We need to create new instances of fluent api everytime we want to log because if it's being used in many places and the values are static they will get overridden
    private String blockMsg;
    private String blockName;

    @Override
    public void msg(String msg) {
        var logger = ScopedVFLImpl.get();
        logger.text(msg);
    }

    @Override
    public SubBlockStep subBlock() {
        return this;
    }


    @Override
    public void run(Runnable runnable) {
        var logger = ScopedVFLImpl.get();
        String localBlockName = blockName;
        if (localBlockName == null) {
            localBlockName = HelperUtil.getLambdaOriginMethodName();
        }
        logger.run(localBlockName, blockMsg, runnable);
    }

    @Override
    public <R> CallStep<R> callable() {
        return null;
    }

    @Override
    public SubBlockStep blockName(String blockName) {
        this.blockName = blockName;
        return this;
    }

    @Override
    public SubBlockStep blockMsg(String msg) {
        this.blockMsg = msg;
        return this;
    }
}
