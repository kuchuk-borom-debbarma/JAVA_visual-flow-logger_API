package dev.kuku.vfl.core.fluent_api.base.steps;

import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.vfl_abstracts.VFL;

public class RunnableStep {
    protected final Runnable runnable;
    private final VFL vfl;

    public RunnableStep(Runnable runnable, VFL vfl) {
        this.runnable = runnable;
        this.vfl = vfl;
    }

    public void andLog(String message, Object... args) {
        runnable.run();
        vfl.log(Util.FormatMessage(message, args));
    }

    public void andWarn(String message, Object... args) {
        runnable.run();
        vfl.warn(Util.FormatMessage(message, args));
    }

    public void andError(String message, Object... args) {
        runnable.run();
        vfl.error(Util.FormatMessage(message, args));
    }
}
