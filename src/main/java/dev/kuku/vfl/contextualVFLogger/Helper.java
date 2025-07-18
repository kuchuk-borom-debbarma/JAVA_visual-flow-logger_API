package dev.kuku.vfl.contextualVFLogger;

import java.util.function.Function;

class Helper {

    public static <R> R blockFnHandler(String blockName, String message, Function<R, String> endMessageFn, Function<ContextualVFL, R> callable, ContextualVFL subLogger) {
        R result = null;
        try {
            result = callable.apply(subLogger);
        } catch (Exception e) {
            subLogger.error("Exception : " + e.getClass().getSimpleName() + " - " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            String endMessage = null;
            if (endMessageFn != null) {
                try {
                    endMessage = endMessageFn.apply(result);
                } catch (Exception e) {
                    endMessage = String.format("Failed to process end message %s - %s", e.getClass().getSimpleName(), e.getMessage());
                }
            }
            subLogger.closeBlock(endMessage);
        }
        return result;
    }
}
