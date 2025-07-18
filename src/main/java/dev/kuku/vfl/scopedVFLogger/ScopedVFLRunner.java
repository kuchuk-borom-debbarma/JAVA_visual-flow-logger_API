package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.scopedVFLogger.Helper.subBlockFnHandler;

public class ScopedVFLRunner {
    private ScopedVFLRunner() {
    }

    private static ScopedVFLContext createScopedLoggerData(String blockName, VFLBuffer buffer) {
        return new ScopedVFLContext(
                new BlockData(UUID.randomUUID().toString(), null, blockName),
                buffer
        );
    }

    public static <V> V call(String blockName, Function<V, String> endMessageFn, VFLBuffer buffer, Callable<V> callable) {
        var scopedLoggerData = createScopedLoggerData(blockName, buffer);
        buffer.pushBlockToBuffer(scopedLoggerData.blockInfo);
        try {
            return subBlockFnHandler(blockName, null, callable, scopedLoggerData);
        } finally {
            buffer.flushAndClose();
        }
    }

    public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
        ScopedVFLRunner.call(blockName, null, buffer, () -> {
            runnable.run();
            return null;
        });
    }
}
