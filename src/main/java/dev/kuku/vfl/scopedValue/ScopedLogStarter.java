package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.*;

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
        var scopedLoggerData = createScopedLoggerData(blockName, buffer);
        buffer.pushBlockToBuffer(scopedLoggerData.blockInfo);
        ScopedValue.where(ScopedValueLoggerData.scopedBlockData, scopedLoggerData)
                .run(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        ScopedLogger.get().error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                    } finally {
                        ScopedLogger.get().closeBlock(null);
                        buffer.shutdown();
                    }
                });
    }

    public static <V> V call(String blockName, Function<V, String> endMessageFn, VFLBuffer buffer, Callable<V> callable) {
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
                        buffer.shutdown();
                    }
                    return result;
                });
    }
}
