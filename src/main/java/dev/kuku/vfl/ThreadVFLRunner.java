package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;

public class ThreadVFLRunner extends VFLRunnerBase {
    public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> fn) {
        VFLBlockContext rootCtx = initRootBlock(blockName, buffer);
        ThreadVFL parentLogger = new ThreadVFL(rootCtx);
        try {
            return ThreadVFL.SetupNewThreadLoggerStackAndCall(parentLogger, fn);
        } finally {
            cleanup(buffer);
        }
    }

    public static <R> void run(String blockName, VFLBuffer buffer, Runnable fn) {
        ThreadVFLRunner.call(blockName, buffer, () -> {
            fn.run();
            return null;
        });
    }
}