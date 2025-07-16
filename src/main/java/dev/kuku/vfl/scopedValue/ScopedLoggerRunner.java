package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.blockFnHandler;
import static dev.kuku.vfl.scopedValue.ScopedValueLoggerData.scopedBlockData;

public class ScopedLoggerRunner {
    private ScopedLoggerRunner() {
    }

    private static BoundedLogData createScopedLoggerData(String blockName, VFLBuffer buffer) {
        return new BoundedLogData(
                new BlockData(UUID.randomUUID().toString(), null, blockName),
                buffer
        );
    }

    public static <V> V call(String blockName, Function<V, String> endMessageFn, VFLBuffer buffer, Callable<V> callable) {
        var scopedLoggerData = createScopedLoggerData(blockName, buffer);
        buffer.pushBlockToBuffer(scopedLoggerData.blockInfo);
        return ScopedValue.where(scopedBlockData,
                        scopedLoggerData)
                .call(() -> {
                    try {
                        return blockFnHandler(callable, null, ScopedLogger.get());
                    } finally {
                        buffer.shutdown();
                    }
                });
    }

    public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
        ScopedLoggerRunner.call(blockName, null, buffer, () -> {
            runnable.run();
            return null;
        });
    }
}
