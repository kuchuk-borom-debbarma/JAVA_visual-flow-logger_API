package dev.kuku.vfl.scoped.fluent;

import dev.kuku.vfl.scoped.ScopedVFL;

import java.util.Arrays;
import java.util.stream.Collectors;

class RunBlockStep implements IRunBlockStep {
    private final Runnable runnable;
    private String blockName;
    private String blockStartMsg = null;

    public RunBlockStep(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        String bn = blockName;
        if (bn == null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

            bn = Arrays.stream(stackTrace).map(s -> s.getMethodName() + "\n").collect(Collectors.joining());
        }
        ScopedVFL.get().run(bn, blockStartMsg, runnable);
    }

    @Override
    public IRunBlockStep withBlockName(String blockName) {
        this.blockName = blockName;
        return this;
    }

    @Override
    public IRunBlockStep withMsg(String message) {
        this.blockStartMsg = message;
        return this;
    }
}
