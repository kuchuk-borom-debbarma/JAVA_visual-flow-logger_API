package dev.kuku.vfl.contextualVFLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;

public class ContextualVFLRunner {

    public static void run(String blockName, VFLBuffer buffer, Consumer<ContextualVFL> fn) {
        ContextualVFLRunner.call(blockName, buffer, (l) -> {
            fn.accept(l);
            return null;
        });
    }

    public static <R> R call(String blockName, VFLBuffer buffer, Function<ContextualVFL, R> fn) {
        var parentBlock = new BlockData(generateUID(), null, blockName);
        var rootLogger = new ContextualVFLImpl(parentBlock, buffer);
        buffer.pushBlockToBuffer(parentBlock);
        R result;
        try {
            result = Helper.blockFnHandler(blockName, null, null, fn, rootLogger);
        } finally {
            buffer.flushAndClose();
        }
        return result;
    }
}
