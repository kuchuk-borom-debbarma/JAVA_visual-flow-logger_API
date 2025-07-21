package dev.kuku.vfl;

import dev.kuku.vfl.core.IVFL;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class StartBlockHelper {
    /**
     * calls the provided function, on exception logs the error, runs the passed onError fn and re-throws exception. <br>
     * Upon completion closes the block after processing endMsgFn.
     */
    public static <R> R callFnForLogger(Callable<R> callable, Function<R, String> endMsgFn, Consumer<Exception> onError, IVFL logger) {
        R result = null;
        try {
            result = callable.call();
        } catch (Exception e) {
            logger.error(String.format(e.getClass().getSimpleName() + " " + e.getMessage()));
            if (onError != null) {
                onError.accept(e);
            }
            throw new RuntimeException(e);
        } finally {
            String endMsg = null;
            if (endMsgFn != null) {
                try {
                    endMsg = endMsgFn.apply(result);
                } catch (Exception e) {
                    endMsg = String.format("Failed processing end message : " + e.getClass().getSimpleName() + " " + e, e.getMessage());
                }
            }
            logger.closeBlock(endMsg);
        }
        return result;
    }

    /**
     * Same as {@link StartBlockHelper#callFnForLogger(Callable, Function, Consumer, IVFL)} but doesn't return a valud and has no endMessage.
     */
    public static <R> void runFnForLogger(Runnable runnable, Consumer<Exception> onError, IVFL logger) {
        StartBlockHelper.callFnForLogger(() -> {
            runnable.run();
            return null;
        }, null, onError, logger);
    }

}