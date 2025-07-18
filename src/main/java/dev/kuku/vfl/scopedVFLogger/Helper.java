package dev.kuku.vfl.scopedVFLogger;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.scopedVFLogger.ScopedValueVFLContext.scopedBlockContext;

class Helper {
    public static <R> R subBlockFnHandler(String blockName, Function<R, String> endMessageFn, Callable<R> callable, ScopedVFLContext scopedValueBlockContext) {
        return ScopedValue.where(scopedBlockContext, scopedValueBlockContext)
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
