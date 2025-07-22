package dev.kuku.vfl.scoped;

import dev.kuku.vfl.BlockHelper;
import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;
import static dev.kuku.vfl.scoped.ScopedVFL.scopedInstance;

public interface IScopedVFL extends IVFL {
    void run(String blockName, String blockMessage, Runnable runnable);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable, Executor executor);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable);

    <R> R call(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable, Executor executor);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable);

    class ScopedVFLRunner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> fn) {
            var rootBlockContext = new BlockData(generateUID(), null, blockName);
            buffer.pushBlockToBuffer(rootBlockContext);
            var vflContext = new VFLBlockContext(rootBlockContext, buffer);
            IScopedVFL rootScope = new ScopedVFL(vflContext);
            try {
                return ScopedValue.where(scopedInstance, rootScope)
                        .call(() -> BlockHelper.CallFnForLogger(fn, null, null, rootScope));
            } finally {
                buffer.flushAndClose();
            }
        }

        public static void run(String blockName, VFLBuffer buffer, Runnable runnable) {
            ScopedVFLRunner.call(blockName, buffer, () -> {
                runnable.run();
                return null;
            });
        }
    }
}