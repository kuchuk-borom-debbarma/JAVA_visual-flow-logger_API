package dev.kuku.vfl.scopedLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class ScopedLoggerRunner {
    private ScopedLoggerRunner() {
    }

    private static ScopedBlockContext createScopedLoggerData(String blockName, VFLBuffer buffer) {
        return new ScopedBlockContext(
                new BlockData(UUID.randomUUID().toString(), null, blockName),
                buffer
        );
    }

    public static <V> V call(String blockName, Function<V, String> endMessageFn, VFLBuffer buffer, Callable<V> callable) {
        var scopedLoggerData = createScopedLoggerData(blockName, buffer);
        buffer.pushBlockToBuffer(scopedLoggerData.blockInfo);
        return ScopedLoggerUtil.subBlockFnHandler(blockName, null, callable, scopedLoggerData);
    }

    public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
        ScopedLoggerRunner.call(blockName, null, buffer, () -> {
            runnable.run();
            return null;
        });
    }
}
