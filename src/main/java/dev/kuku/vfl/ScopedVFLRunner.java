package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;

import static dev.kuku.vfl.ScopedVFL.scopedInstance;

public class ScopedVFLRunner extends VFLRunnerBase {
    public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> fn) {
        VFLBlockContext rootCtx = initRootBlock(blockName, buffer);
        IScopedVFL rootScope = new ScopedVFL(rootCtx);
        try {
            return ScopedValue.where(scopedInstance, rootScope)
                    .call(() -> BlockHelper.CallFnForLogger(fn, null, null, rootScope));
        } finally {
            cleanup(buffer);
        }
    }

    public static <R> void run(String blockName, VFLBuffer buffer, Runnable fn) {
        ScopedVFLRunner.call(blockName, buffer, () -> {
            fn.run();
            return null;
        });
    }
}