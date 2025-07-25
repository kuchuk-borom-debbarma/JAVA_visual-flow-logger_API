package dev.kuku.vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlcokStartEnum;
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
    protected void afterSubBlockAndLogCreatedAndPushed2Buffer(Block createdSubBlock, SubBlockStartLog createdSubBlockStartLog, LogTypeBlcokStartEnum startType) {
        if (startType != LogTypeBlcokStartEnum.SUB_BLOCK_START_PRIMARY) {
            //Starting a concurrent block so it will be a new thread
            Stack<ThreadVFL> stack = new Stack<>();
            ThreadVFL.loggerStack.set(stack);
        }
        var subBlockLogger = new ThreadVFL(new VFLBlockContext(createdSubBlock, ctx.buffer, ctx.allowedLogTypes));
        ThreadVFL.loggerStack.get().push(subBlockLogger);
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