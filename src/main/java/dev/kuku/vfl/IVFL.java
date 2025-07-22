package dev.kuku.vfl;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.VFLBlockContext;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

/**
 * Simple logger for logging strings.
 */
public interface IVFL {
    void msg(String message);

    <R> R msgFn(Callable<R> fn, Function<R, String> messageFn);

    void warn(String message);

    <R> R warnFn(Callable<R> fn, Function<R, String> messageFn);

    void error(String message);

    <R> R errorFn(Callable<R> fn, Function<R, String> messageFn);

    void closeBlock(String endMessage);

    class VFLRunner {
        public static <R> R call(String blockName, VFLBuffer buffer, Function<IVFL, R> fn, Function<R, String> endMsgFn) {
            var rootBlock = BlockHelper.CreateBlockDataAndPush(generateUID(), blockName, null, buffer);
            var rootContext = new VFLBlockContext(rootBlock, buffer);
            var logger = new VFL(rootContext);
            try {
                return BlockHelper.CallFnForLogger(() -> fn.apply(logger), endMsgFn, null, logger);
            } finally {
                buffer.flushAndClose();
            }
        }

        public static void run(String blockName, VFLBuffer buffer, Consumer<IVFL> fn) {
            VFLRunner.call(blockName, buffer, ivfl -> {
                fn.accept(ivfl);
                return null;
            }, null);
        }
    }
}
//TODO Thread safe async logger using virtual threads. The one i amde rn is very buggy and needs to be redone
//TODO local file flush handler
//TODO annotation based flow logger
//TODO figure out how we can display forked block which joins back
//TODO take in list of flushHandler and flush to all of them using new flush type
//TODO different level for filtering
//TODO common class for simple stuffs
//TODO compile time flow generation for flow chart

/*
 * // Even more generic approach using builder pattern
public class VFLRunnerBuilder<T extends IVFL> {
    private String blockName;
    private VFLBuffer buffer;
    private VFLFactory<T> factory;

    public VFLRunnerBuilder(String blockName, VFLBuffer buffer) {
        this.blockName = blockName;
        this.buffer = buffer;
    }

    public VFLRunnerBuilder<T> withFactory(VFLFactory<T> factory) {
        this.factory = factory;
        return this;
    }

    public <R> R execute(VFLExecutor<T, R> executor) {
        return BaseVFLRunner.call(blockName, buffer, factory, executor);
    }

    public <R> R execute(Function<T, R> function) {
        return execute(function::apply);
    }

    public <R> R execute(Callable<R> callable) {
        return execute(vfl -> callable.call());
    }

    public void run(Runnable runnable) {
        execute(vfl -> {
            runnable.run();
            return null;
        });
    }
}

// Builder usage example
class BuilderExample {
    public static void example() {
        VFLBuffer buffer = new VFLBuffer();

        String result = new VFLRunnerBuilder<IVFL>("my-block", buffer)
            .withFactory(VFLFactories.VFL_FACTORY)  // Use factory constant
            .execute(vfl -> {
                vfl.msg("Hello World");
                return "completed";
            });

        new VFLRunnerBuilder<IPassthroughVFL>("another-block", buffer)
            .withFactory(VFLFactories.PASSTHROUGH_VFL_FACTORY)  // Use factory constant
            .run(() -> System.out.println("Running passthrough logic"));
    }
}
 */