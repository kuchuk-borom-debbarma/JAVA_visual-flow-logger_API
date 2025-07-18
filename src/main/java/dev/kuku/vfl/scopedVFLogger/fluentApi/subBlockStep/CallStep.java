package dev.kuku.vfl.scopedVFLogger.fluentApi.subBlockStep;

import java.util.concurrent.Callable;
import java.util.function.Function;

public interface CallStep<R> {
    /**
     * Call the passed method and return it's value
     *
     * @param callable the method to call
     */
    R call(Callable<R> callable);

    /**
     * Set the end message
     *
     * @param endMsgFn function that takes in the result of the callable and returns the end message
     */
    CallStep<R> endMsg(Function<R, String> endMsgFn);
}
