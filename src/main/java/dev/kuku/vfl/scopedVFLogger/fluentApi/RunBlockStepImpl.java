package dev.kuku.vfl.scopedVFLogger.fluentApi;

import dev.kuku.vfl.core.util.HelperUtil;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Collectors;

class RunBlockStepImpl implements RunBlockStep {
    private final Runnable runnable;
    private String blockName;
    private String blockStartMsg = null;

    public RunBlockStepImpl(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        String bn = blockName;
        if (bn == null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            bn = Arrays.stream(stackTrace).map(s-> s.getMethodName()+"\n").collect(Collectors.joining());
        }
        ScopedVFLImpl.get().run(bn, blockStartMsg, runnable);
    }

    @Override
    public RunBlockStep withBlockName(String blockName) {
        this.blockName = blockName;
        return this;
    }

    @Override
    public RunBlockStep withMsg(String message) {
        this.blockStartMsg = message;
        return this;
    }
}
