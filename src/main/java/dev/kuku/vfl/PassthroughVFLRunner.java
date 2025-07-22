package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.function.Consumer;
import java.util.function.Function;

public class PassthroughVFLRunner extends VFLRunnerBase {
    public static <R> R call(String blockName, VFLBuffer buffer, Function<IPassthroughVFL, R> fn) {
        VFLBlockContext rootCtx = initRootBlock(blockName, buffer);
        IPassthroughVFL logger = new PassthroughVFL(rootCtx);
        try {
            return BlockHelper.CallFnForLogger(() -> fn.apply(logger), null, null, logger);
        } finally {
            cleanup(buffer);
        }
    }

    public static <R> void run(String blockName, VFLBuffer buffer, Consumer<IPassthroughVFL> fn) {
        PassthroughVFLRunner.call(blockName, buffer, (a) -> {
            fn.accept(a);
            return null;
        });
    }
}