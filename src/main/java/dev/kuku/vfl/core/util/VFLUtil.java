package dev.kuku.vfl.core.util;

import dev.kuku.vfl.core.BlockLog;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class VFLUtil {
    public static String generateUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Handles running the sub block function passed and closes it when complete.<br>
     * If exception is thrown by the passed function, exception is logged, and exception is re-thrown.
     *
     * @param callable          the function to be called that returns a value
     * @param endMessageFn      message to set for ending log
     * @param subLoggerInstance subLoggerInstance instance which is being used by the sub block
     */
    public static <R> R blockFnHandler(Callable<R> callable, Function<R, String> endMessageFn, BlockLog subLoggerInstance) {
        R result = null;
        try {
            result = callable.call();
        } catch (Throwable e) {
            //Use sub logger instance to write down the error and rethrow it
            subLoggerInstance.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
            throw new RuntimeException(e);
        } finally {
            //Close the block with endMessage(if valid)
            String endMessage = null;
            if (endMessageFn != null) {
                try {
                    endMessage = endMessageFn.apply(result);
                } catch (Exception e) {
                    endMessage = "Error processing End Message : " + String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage());
                }
            }
            subLoggerInstance.closeBlock(endMessage);
        }
        return result;
    }

    public static <R> void blockFnHandler(Runnable runnable, Function<R, String> endMessageFn, BlockLog subLoggerInstance) {
        VFLUtil.blockFnHandler(() -> {
            runnable.run();
            return null;
        }, endMessageFn, subLoggerInstance);
    }
}
