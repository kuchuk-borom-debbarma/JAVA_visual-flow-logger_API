package dev.kuku.vfl.scoped;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public interface IScopedVFL extends IVFL {
    void run(String blockName, String blockMessage, Runnable runnable);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable, Executor executor);

    <R> R call(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable, Executor executor);

    class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> fn) {
            var rootBlockContext = new BlockData(generateUID(), null, blockName);
            buffer.pushBlockToBuffer(rootBlockContext);
            var vflContext = new VFLBlockContext(rootBlockContext, buffer);
            IScopedVFL IScopedVFL = new ScopedVFL(vflContext);
            try {
                return Helper.blockFnLifeCycleHandler(blockName, null, fn, IScopedVFL);
            } finally {
                buffer.flushAndClose();
            }
        }

        public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
            Runner.call(blockName, buffer, () -> {
                runnable.run();
                return null;
            });
        }
    }
}