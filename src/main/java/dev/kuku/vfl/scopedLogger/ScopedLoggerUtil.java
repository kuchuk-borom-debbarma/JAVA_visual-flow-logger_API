package dev.kuku.vfl.scopedLogger;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.scopedLogger.ScopedValueBlockContext.scopedBlockContext;

class ScopedLoggerUtil {
    public static <R> R subBlockFnHandler(String blockName, Function<R, String> endMessageFn, Callable<R> callable, ScopedBlockContext scopedValueBlockContext) {
        return ScopedValue.where(scopedBlockContext, scopedValueBlockContext)
                .call(
                        () -> {
                            R result = null;
                            try {
                                result = callable.call();
                            } catch (Exception e) {
                                ScopedLoggerImpl.get().error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
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
                                System.out.println("Ending block " + scopedValueBlockContext.blockInfo.getId());
                                ScopedLoggerImpl.get().closeBlock(endMessage);
                            }
                            return result;
                        }
                );
    }
}
