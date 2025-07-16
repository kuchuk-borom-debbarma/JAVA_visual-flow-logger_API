package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;
import java.util.concurrent.Callable;

public class ScopedLogStarter {
    private ScopedLogStarter() {
    }

    private static ScopedLoggerData createScopedLoggerData(String blockName, VFLBuffer buffer) {
        return new ScopedLoggerData(
                new BlockData(UUID.randomUUID().toString(), null, blockName),
                buffer
        );
    }

    public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
        ScopedValue.where(ScopedValueLoggerData.scopedBlockData, createScopedLoggerData(blockName, buffer))
                .run(runnable);
    }

    public <V> V call(String blockName, VFLBuffer buffer, Callable<V> callable) {
        return ScopedValue.where(ScopedValueLoggerData.scopedBlockData, createScopedLoggerData(blockName, buffer))
                .call(() -> {
                    V result = null;
                    try {
                        result = callable.call();
                    } catch (Exception e) {
                        //TODO log exception
                    } finally {
                        //TODO close block
                        ScopedLogger.get().closeBlock(null);
                    }
                    return result;
                });
    }
}
