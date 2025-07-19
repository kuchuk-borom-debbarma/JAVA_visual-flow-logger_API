package dev.kuku.vfl.passthrough;

import java.util.function.Function;

public class Helper {

    public static <R> R blockFnLifeCycleHandler(Function<IPassthroughVFL, R> fn, Function<R, String> endMessageFn, IPassthroughVFL subBlockLogger) {
        R result = null;
        try {
            result = fn.apply(subBlockLogger);
        } catch (Exception e) {
            subBlockLogger.error("Exception : " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            String endMsg = null;
            if (endMessageFn != null) {
                try {
                    endMsg = endMessageFn.apply(result);
                } catch (Exception e) {
                    endMsg = String.format("Failed to process end message %s - %s", e.getClass().getSimpleName(), e.getMessage());
                }
            }
            subBlockLogger.closeBlock(endMsg);
        }
        return result;
    }
}
