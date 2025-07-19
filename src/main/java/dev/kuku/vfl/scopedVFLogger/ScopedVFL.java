package dev.kuku.vfl.scopedVFLogger;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public interface ScopedVFL extends VFL {
    void run(String blockName, String blockMessage, Runnable runnable);

    CompletableFuture<Void> runAsync(String blockName, String blockMessage, Runnable runnable, Executor executor);

    <R> R call(String blockName, String blockMessage, Function<R, String> endMessageFn, Callable<R> callable);

    <R> CompletableFuture<R> callAsync(String blockName, String blockMessage,Function<R, String> endMessageFn, Callable<R> callable, Executor executor);

    class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> fn) {
            var rootBlockContext = new BlockData(generateUID(), null, blockName);
            buffer.pushBlockToBuffer(rootBlockContext);
            var vflContext = new VFLBlockContext(rootBlockContext, buffer);
            ScopedVFL scopedVFL = new ScopedVFLImpl(vflContext);
            try {
                return Helper.subBlockFnHandler(blockName, null, fn, scopedVFL);
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
//TODO_OLD support for multi thread environment by explicitly passing context. DONE! we created async version of the methods and pass the conext to the thread where it will be running
//TODO throw exceptions for fn calls but handle it gracefully within the logger too
//TODO_OLD use synchronise? research and think properly before deciding this ANSWER = NO
//TODO fluent api to reduce verbosity and to get rid of overloaded functions