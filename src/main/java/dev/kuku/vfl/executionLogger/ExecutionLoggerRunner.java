package dev.kuku.vfl.executionLogger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;

import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;

public class ExecutionLoggerRunner {

    public static void run(String blockName, VFLBuffer buffer, Consumer<ExecutionLogger> fn) {
        ExecutionLoggerRunner.call(blockName, buffer, (l) -> {
            fn.accept(l);
            return null;
        });
    }

    public static <R> R call(String blockName, VFLBuffer buffer, Function<ExecutionLogger, R> fn) {
        var rootLogger = new ExecutionLoggerImpl(new BlockData(generateUID(), null, blockName), buffer);
        R result;
        try {
            result = fn.apply(rootLogger);
        } catch (Exception e) {
            rootLogger.error(String.format("Exception %s : %s", e.getClass().getName(), e.getMessage()));
            throw new RuntimeException(e);
        } finally {
            rootLogger.closeBlock(null);
            buffer.shutdown();
        }
        return result;
    }
}
