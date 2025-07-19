package dev.kuku.vfl.scopedVFLogger;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.scopedVFLogger.ScopedValueVFLContext.scopedInstance;

class Helper {
    /**
     * Creates a new scope boundary with the passed {@link ScopedVFL} instance. <br>
     * Then, runs the passed method and closes the block once method execution is complete. <br>
     * If any exception is thrown, the exception is logged in the chain and re-thrown.
     * @param blockName name of the block
     * @param endMessageFn function to process the end message for log dictating the end of the block
     * @param callable method to call
     * @param scopedVFL instance that will be set as the new scope's instance
     * @param <R> return value of callable method
     */
    public static <R> R subBlockFnHandler(String blockName, Function<R, String> endMessageFn, Callable<R> callable, ScopedVFL scopedVFL) {
        Objects.requireNonNull(scopedVFL);
        return ScopedValue.where(scopedInstance, scopedVFL)
                .call(
                        () -> {
                            R result = null;
                            try {
                                result = callable.call();
                            } catch (Exception e) {
                                ScopedVFLImpl.get().error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                                throw new RuntimeException(e);
                            } finally {
                                String endMessage = null;
                                if (endMessageFn != null) {
                                    try {
                                        endMessage = endMessageFn.apply(result);
                                    } catch (Exception e) {
                                        endMessage = String.format("Failed to process end message %s : %s", e.getClass().getSimpleName(), e.getMessage());
                                    }
                                }
                                ScopedVFLImpl.get().closeBlock(endMessage);
                            }
                            return result;
                        }
                );
    }
}
