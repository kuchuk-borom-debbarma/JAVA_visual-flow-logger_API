package dev.kuku.vfl.core.util;

import dev.kuku.vfl.core.BlockLog;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class VFLUtil {
    public static String generateUID() {
        return UUID.randomUUID().toString();
    }

    public static <R> R toBeCalledFn(Callable<R> callable, Function<R, String> endMessageFn, BlockLog logger) throws Exception {
        R result = null;
        try {
            result = callable.call();
        } catch (Throwable e) {
            logger.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
            throw e;
        } finally {
            String endMessage = null;
            if (endMessageFn != null) {
                try {
                    endMessage = endMessageFn.apply(result);
                } catch (Exception e) {
                    endMessage = "Error processing End Message : " + String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage());
                }
            }
            logger.closeBlock(endMessage);
        }
        return result;
    }
}
