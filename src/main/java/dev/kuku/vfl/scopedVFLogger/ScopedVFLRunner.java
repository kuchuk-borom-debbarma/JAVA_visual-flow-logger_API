package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;
import static dev.kuku.vfl.scopedVFLogger.Helper.subBlockFnHandler;

public class ScopedVFLRunner {
    private ScopedVFLRunner() {
    }

    public static <V> V call(String blockName, Function<V, String> endMessageFn, VFLBuffer buffer, Callable<V> callable) {
        //Create parent block context
        VFLBlockContext scopedLoggerData = new VFLBlockContext(new BlockData(generateUID(), null, blockName), buffer);
        //Push parent block to buffer
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
