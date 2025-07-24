package dev.kuku.vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;

import java.util.Stack;
import java.util.concurrent.Callable;

public class ThreadVFL extends VFLCallable {
    private static final ThreadLocal<Stack<ThreadVFL>> loggerStack = new ThreadLocal<>();
    private final VFLBlockContext ctx;

    private ThreadVFL(VFLBlockContext context) {
        this.ctx = context;
    }

    @Override
    public ThreadVFL getLogger() {
        return loggerStack.get().peek();
    }

    @Override
    protected VFLBlockContext getContext() {
        return getLogger().ctx;
    }

    @Override
    public void close(String endMessage) {
        super.close(endMessage);
        ThreadVFL.loggerStack.get().pop();
        if (ThreadVFL.loggerStack.get().isEmpty()) {
            ThreadVFL.loggerStack.remove();
        }
    }

    static class Runner extends VFLRunner {
        public static <R> R call(String blockName, VFLBuffer buffer, Callable<R> callable) {
            var rootLogger = new ThreadVFL(initRootCtx(blockName, buffer));
            //Create logger stack
            Stack<ThreadVFL> stack = new Stack<>();
            stack.push(rootLogger);
            ThreadVFL.loggerStack.set(stack);
            try {
                return VFLHelper.CallFnWithLogger(callable, rootLogger, null);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}