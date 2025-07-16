package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.toBeCalledFn;
import static dev.kuku.vfl.scopedValue.ScopedValueLoggerData.scopedBlockData;

public class ScopedLogStarter {
    private ScopedLogStarter() {
    }

    private static BoundedLogData createScopedLoggerData(String blockName, VFLBuffer buffer) {
        return new BoundedLogData(
                new BlockData(UUID.randomUUID().toString(), null, blockName),
                buffer
        );
    }

    public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
        var scopedLoggerData = createScopedLoggerData(blockName, buffer);
        buffer.pushBlockToBuffer(scopedLoggerData.blockInfo);
        //Important to wrap the function call OR else the function will not be within the scope
        //.run(toBeCalledFn()) will not work
        ScopedValue.where(scopedBlockData, scopedLoggerData)
                .run(() -> {
                    try {
                        toBeCalledFn(() -> {
                            runnable.run();
                            return null;
                        }, null, ScopedLogger.get());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        //Flush everything
                        buffer.shutdown();
                    }
                });
    }

    public static <V> V call(String blockName, Function<V, String> endMessageFn, VFLBuffer buffer, Callable<V> callable) {
        var scopedLoggerData = createScopedLoggerData(blockName, buffer);
        buffer.pushBlockToBuffer(scopedLoggerData.blockInfo);
        try {
            return ScopedValue.where(scopedBlockData,
                            scopedLoggerData)
                    .call(() -> toBeCalledFn(() -> {
                        try {
                            return callable.call();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        } finally {
                            buffer.shutdown();
                        }
                    }, endMessageFn, ScopedLogger.get()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
