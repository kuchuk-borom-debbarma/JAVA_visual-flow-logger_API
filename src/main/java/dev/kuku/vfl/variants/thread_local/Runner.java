package dev.kuku.vfl.variants.thread_local;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.EventPublisherBlock;
import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.vfl_abstracts.VFL;
import dev.kuku.vfl.core.vfl_abstracts.runner.VFLCallableRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;
import java.util.function.Supplier;

import static dev.kuku.vfl.core.helpers.Util.getThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.trimId;

@Slf4j
public class Runner extends VFLCallableRunner {
    private final static Runner INSTANCE = new Runner();

    public static <R> R StartVFL(String blockName, VFLBuffer buffer, Supplier<R> fn) {
        return INSTANCE.startVFL(blockName, buffer, fn);
    }

    public static void StartVFL(String blockName, VFLBuffer buffer, Runnable runnable) {
        INSTANCE.startVFL(blockName, buffer, runnable);
    }

    public static void StartEventListenerLogger(String eventListenerName, String eventStartMessage, VFLBuffer buffer, EventPublisherBlock eventData, Runnable r) {
        INSTANCE.startEventListenerLogger(eventListenerName, eventStartMessage, buffer, eventData, r);
    }

    @Override
    protected VFL createRootLogger(VFLBlockContext rootCtx) {
        var rootLogger = new ThreadVFL(rootCtx);
        String threadInfo = getThreadInfo();

        //Create logger stack
        Stack<ThreadVFL> stack = new Stack<>();
        stack.push(rootLogger);
        ThreadVFL.loggerStack.set(stack);
        log.debug("CREATE ROOT: Created root logger '{}' and initialized stack {} - Stack size: {}", trimId(rootLogger.ctx.blockInfo.getId()), threadInfo, stack.size());
        return rootLogger;
    }

    @Override
    protected VFL createEventListenerLogger(VFLBlockContext eventListenerCtx) {
        ThreadVFL eventListenerBlockLogger = new ThreadVFL(eventListenerCtx);
        String threadInfo = getThreadInfo();

        /*
         * Depending on the implementation of the event publisher and subscribe implementation this may or may not be running in a separate thread.
         *
         * If it's in the same thread as caller then it will be sequential in nature and thus can be added to the stack which pops once operation is complete.
         *
         * If it's running in a different thread the loggerStack should be null as secondaryBlockStart calls always clean up after they are done.
         */
        if (ThreadVFL.loggerStack.get() == null) {
            Stack<ThreadVFL> newStack = new Stack<>();
            ThreadVFL.loggerStack.set(newStack);
            newStack.push(eventListenerBlockLogger);
            log.debug("CREATE EVENT: Created new stack for event listener '{}' {} - Stack size: {}", trimId(eventListenerBlockLogger.ctx.blockInfo.getId()), threadInfo, newStack.size());
        } else {
            ThreadVFL.loggerStack.get().push(eventListenerBlockLogger);
            log.debug("PUSH EVENT: Added event listener '{}' to existing stack {} - Stack size: {}", trimId(eventListenerBlockLogger.ctx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());
        }

        return ThreadVFL.loggerStack.get().peek();
    }
}
