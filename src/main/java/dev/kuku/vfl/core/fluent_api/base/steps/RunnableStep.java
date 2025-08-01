package dev.kuku.vfl.core.fluent_api.base.steps;

import dev.kuku.vfl.core.vfl_abstracts.VFL;

public class RunnableStep {
    protected final Runnable runnable;
    private final VFL vfl;

    public RunnableStep(Runnable runnable, VFL vfl) {
        this.runnable = runnable;
        this.vfl = vfl;
    }

    public void andLog(String message) {
        runnable.run();
        vfl.log(message);
    }

    public void andWarn(String message) {
        runnable.run();
        vfl.warn(message);
    }

    public void andError(String message) {
        runnable.run();
        vfl.error(message);
    }
}
