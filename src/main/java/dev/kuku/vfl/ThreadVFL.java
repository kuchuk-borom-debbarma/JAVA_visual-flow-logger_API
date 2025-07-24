package dev.kuku.vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.concurrent.Callable;

public class ThreadVFL extends VFLCallable {
    private static final ThreadLocal<ThreadVFL> instance = new ThreadLocal<>();
    private final VFLBlockContext ctx;

    private ThreadVFL(VFLBlockContext context) {
        this.ctx = context;
    }

    private static void SetThreadInstance(ThreadVFL value) {
        if (instance.get() != null) {
            throw new IllegalStateException("instance is already set!");
        }
        instance.set(value);
    }

    @Override
    public ThreadVFL getLogger() {
        return instance.get();
    }

    @Override
    protected VFLBlockContext getContext() {
        return getLogger().ctx;
    }

    static class Runner extends VFLRunner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> callable) {
            var rootLogger = new ThreadVFL(initRootCtx(blockName, buffer));
            //Set current thread's instance
            ThreadVFL.SetThreadInstance(rootLogger);
            try {
                return VFLHelper.CallFnWithLogger(callable, rootLogger, null);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}