package dev.kuku.vfl.scopedVFLogger.fluentApi;

import dev.kuku.vfl.scopedVFLogger.ScopedVFL;
import dev.kuku.vfl.scopedVFLogger.ScopedVFLImpl;

import java.util.concurrent.Callable;

public class ScopedFluent {
    private static final ScopedVFL logger = ScopedVFLImpl.get();

    // Private constructor to prevent instantiation
    private ScopedFluent() {
    }

    public static void msg(String msg) {
        logger.msg(msg);
    }

    public static void error(String error) {
        logger.error(error);
    }

    public static void warn(String warn) {
        logger.warn(warn);
    }

    public static RunBlockStep subBlockRunner(Runnable runnable) {
        return new RunBlockStepImpl(runnable);
    }

    public static <R> CallBlockStep<R> callBlock(Callable<R> callable) {
        return new CallBlockStepImpl<>(callable);
    }
}
