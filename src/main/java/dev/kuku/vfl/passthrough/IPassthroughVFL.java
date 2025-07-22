package dev.kuku.vfl.passthrough;

import dev.kuku.vfl.BlockHelper;
import dev.kuku.vfl.core.IVFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

public interface IPassthroughVFL extends IVFL {

    void run(String blockName, String message, Consumer<IPassthroughVFL> fn);

    CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn, Executor executor);

    CompletableFuture<Void> runAsync(String blockName, String message, Consumer<IPassthroughVFL> fn);

    <R> R call(String blockName, String message, Function<R, String> endMessageFn, Function<IPassthroughVFL, R> fn);

    <R> CompletableFuture<R> callAsync(String blockName, String message, Function<R, String> endMessageFn,
                                       Function<IPassthroughVFL, R> fn, Executor executor);

    <R> CompletableFuture<R> callAsync(String blockName, String message, Function<R, String> endMessageFn,
                                       Function<IPassthroughVFL, R> fn);

    class Runner {
        public static <R> R call(String blockName, VFLBuffer buffer, Function<IPassthroughVFL, R> fn) {
            BlockData rootBlockInfo = new BlockData(generateUID(), null, blockName);
            buffer.pushBlockToBuffer(rootBlockInfo);
            VFLBlockContext rootContext = new VFLBlockContext(rootBlockInfo, buffer);
            IPassthroughVFL rootLogger = new PassthroughVFL(rootContext);
            try {
                return BlockHelper.CallFnForLogger(() -> fn.apply(rootLogger), null, null, rootLogger);
            } finally {
                buffer.flushAndClose();
            }
        }

        public static <R> void run(String blockName, VFLBuffer buffer, Consumer<IPassthroughVFL> fn) {
            Runner.call(blockName, buffer, (l) -> {
                fn.accept(l);
                return null;
            });
        }
    }
}
