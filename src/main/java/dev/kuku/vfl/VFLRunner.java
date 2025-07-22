package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.function.Consumer;
import java.util.function.Function;

public class VFLRunner extends VFLRunnerBase {
    public static <R> R call(String blockName, VFLBuffer buffer, Function<IVFL, R> fn) {
        VFLBlockContext rootCtx = initRootBlock(blockName, buffer);
        IVFL logger = new VFL(rootCtx);
        try {
            return BlockHelper.CallFnForLogger(() -> fn.apply(logger), null, null, logger);
        } finally {
            cleanup(buffer);
        }
    }

    public static <R> void run(String blockName, VFLBuffer buffer, Consumer<IVFL> fn) {
        VFLRunner.call(blockName, buffer, (l) -> {
            fn.accept(l);
            return null;
        });
    }
}