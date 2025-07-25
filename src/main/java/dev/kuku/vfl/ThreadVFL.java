package dev.kuku.vfl;

import dev.kuku.vfl.core.VFLRunner;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.models.EventPublisherBlock;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.logs.SubBlockStartLog;
import dev.kuku.vfl.core.models.logs.enums.LogTypeBlockStartEnum;
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
    protected void afterSubBlockAndLogCreatedAndPushed2Buffer(Block createdSubBlock, SubBlockStartLog createdSubBlockStartLog, LogTypeBlockStartEnum startType) {
        if (startType != LogTypeBlockStartEnum.SUB_BLOCK_START_PRIMARY) {
            //Starting a concurrent block so it will be a new thread
            Stack<ThreadVFL> stack = new Stack<>();
            ThreadVFL.loggerStack.set(stack);
        }
        var subBlockLogger = new ThreadVFL(new VFLBlockContext(createdSubBlock, ctx.buffer));
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
        public static <R> R Call(String blockName, VFLBuffer buffer, Callable<R> callable) {
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

        public static void RunEventListener(String eventListenerName, String eventListenerMessage, EventPublisherBlock eventPublisherBlock, VFLBuffer buffer, Runnable runnable) {
            //Create the event listener block
            var eventListenerBlock = VFLHelper.CreateBlockAndPush2Buffer(eventListenerName, eventPublisherBlock.block().getId(), buffer);
            //Create a log for event pusblisher block of type event listener
            VFLHelper.CreateLogAndPush2Buffer(eventPublisherBlock.block().getId(), null, eventListenerMessage, eventListenerBlock.getId(), LogTypeBlockStartEnum.EVENT_LISTENER, buffer);

            ThreadVFL eventListenerBlockLogger = new ThreadVFL(new VFLBlockContext(eventListenerBlock, buffer));
            if (ThreadVFL.loggerStack.get() == null) {
                ThreadVFL.loggerStack.set(new Stack<>());
            }
            ThreadVFL.loggerStack.get().push(eventListenerBlockLogger);
            try {
                VFLHelper.CallFnWithLogger(() -> {
                    runnable.run();
                    return null;
                }, eventListenerBlockLogger, null);
            } finally {
                buffer.flushAndClose();
            }
        }
    }
}