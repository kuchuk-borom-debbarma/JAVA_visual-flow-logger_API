package dev.kuku.vfl.variants.thread_local;

import dev.kuku.vfl.core.dtos.VFLBlockContext;
import dev.kuku.vfl.core.models.Block;
import dev.kuku.vfl.core.vfl_abstracts.VFLCallable;
import lombok.extern.slf4j.Slf4j;

import static dev.kuku.vfl.core.helpers.Util.getThreadInfo;
import static dev.kuku.vfl.core.helpers.Util.trimId;

@Slf4j
public class ThreadVFL extends VFLCallable {
    public static final InheritableThreadVFLStack loggerStack = new InheritableThreadVFLStack();
    public final VFLBlockContext ctx;

    ThreadVFL(VFLBlockContext context) {
        this.ctx = context;
    }

    /**
     * Get the current logger from the thread local stack
     */
    public static ThreadVFL getCurrentLogger() {
        return loggerStack.get().peek();
    }

    /**
     * After sub block start has been initialized we need to create a logger based on sub block created and push it to the stack. <br>
     * We have to do this because it means that the passed function for the sub block is going to be invoked and when the sub block's function attempts to get the logger will give it the latest logger i.e the logger for the sub block which is exactly what we want
     */
    @Override
    protected void afterSubBlockStartInit(VFLBlockContext parentBlockCtx, Block subBlock) {
        /// inheritedContext is false because this is not an inherited from parent thread. inheritedContext is true only when a child thread is spawned. Check out {@link InheritableThreadVFLStack}
        var subLoggerCtx = new VFLBlockContext(subBlock, false, parentBlockCtx.buffer);
        ThreadVFL newLogger = new ThreadVFL(subLoggerCtx);
        ThreadVFL.loggerStack.get().push(newLogger);
        String threadInfo = getThreadInfo();
        log.debug("PUSH: Added logger '{}' to existing stack {} - Stack size: {}", trimId(subLoggerCtx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());
    }

    /**
     * Method used by super class to get logger instance.
     * Getting the instance through a method allows dynamically determining what to return.
     * In this case the latest logger from the ThreadLocal VFL Stack is returned
     */
    @Override
    protected ThreadVFL getLogger() {
        return getCurrentLogger();
    }

    @Override
    protected VFLBlockContext getContext() {
        return getCurrentLogger().ctx;
    }

    @Override
    protected void close(String endMessage) {
        super.close(endMessage);
        popLoggerFromStack();
    }

    private void popLoggerFromStack() {
        var poppedLogger = ThreadVFL.loggerStack.get().pop();
        String threadInfo = getThreadInfo();
        log.debug("POP: Removed logger '{}' from stack {} - Remaining stack size: {}", trimId(poppedLogger.ctx.blockInfo.getId()), threadInfo, ThreadVFL.loggerStack.get().size());
        //Clean up the thread local if its empty or if the last remaining instance has inherited context
        if (ThreadVFL.loggerStack.get().isEmpty()) {
            ThreadVFL.loggerStack.remove();
            log.debug("REMOVE: Completely removed logger stack from {} - Stack cleaned up", threadInfo);
        }
    }
}