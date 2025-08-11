package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.Block;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

/**
 * Manager thread local context
 */
@Slf4j
class ThreadContextManager {

    static final ThreadLocal<Stack<BlockContext>> loggerCtxStack = new ThreadLocal<>();

    static BlockContext GetCurrentBlockContext() {
        if (loggerCtxStack.get() == null || loggerCtxStack.get().isEmpty()) return null;
        return loggerCtxStack.get().peek();
    }

    static void PopCurrentStack(String endMsg) {
        if (GetCurrentBlockContext() == null) {
            log.warn("Failed to close current context : Logger stack is empty or null. This usually happens when a method annotated with @SubBlock is invoked without parent(by using VFLStarter). Most of the time this should be okay.");
            return;
        }
        Log.INSTANCE.close(endMsg);
        BlockContext popped = loggerCtxStack.get().pop();
        log.debug("Popped current context : {}-{} for thread {}", popped.blockInfo.getBlockName(), Util.TrimId(popped.blockInfo.getId()), Util.GetThreadInfo());

        if (loggerCtxStack.get().isEmpty()) {
            log.debug("LoggerCtxStack is empty for thread {}, Removing thread variable", Util.GetThreadInfo());
            loggerCtxStack.remove();
        }
    }

    /// If stack is null create new stack
    static void PushBlockToThreadLogStack(Block subBlock) {
        if (GetCurrentBlockContext() == null) {
            loggerCtxStack.set(new Stack<>());
        }
        loggerCtxStack.get().push(new BlockContext(subBlock));
    }
}