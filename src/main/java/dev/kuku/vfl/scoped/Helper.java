package dev.kuku.vfl.scoped;

import dev.kuku.vfl.StartBlockHelper;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.scoped.ScopedVFL.scopedInstance;

class Helper {
    /**
     * Creates a new scope boundary with the passed {@link IScopedVFL} instance. <br>
     * Then, runs the passed method and closes the block once method execution is complete. <br>
     * If any exception is thrown, the exception is logged in the chain and re-thrown.
     *
     * @param blockName    name of the block
     * @param endMessageFn function to process the end message for log dictating the end of the block
     * @param callable     method to call
     * @param logger   instance that will be set as the new scope's instance
     * @param <R>          return value of callable method
     */
    public static <R> R blockFnLifeCycleHandler(String blockName, Function<R, String> endMessageFn, Callable<R> callable, IScopedVFL logger) {
        Objects.requireNonNull(logger);
        return ScopedValue.where(scopedInstance, logger)
                .call(
                        () -> StartBlockHelper.CallFnForLogger(callable, endMessageFn, null, logger)
                );
    }
}
