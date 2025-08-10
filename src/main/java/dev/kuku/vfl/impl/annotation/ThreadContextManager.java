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

    static void CleanThreadVariables() {
        loggerCtxStack.remove();
    }

    static void InitializeStackWithBlock(Block block) {
        loggerCtxStack.set(new Stack<>());
        loggerCtxStack.get().push(new BlockContext(block, false));
    }

    static void InitializeStackWithContext(BlockContext blockContext) {
        loggerCtxStack.set(new Stack<>());
        loggerCtxStack.get().push(blockContext);
    }

    static BlockContext GetCurrentBlockContext() {
        if (loggerCtxStack.get() == null || loggerCtxStack.get().isEmpty()) return null;
        return loggerCtxStack.get().peek();
    }

    static void CloseAndPopCurrentContext(String endMsg) {
        if(GetCurrentBlockContext() == null){
            log.warn("Failed to close current context : Logger stack is empty or null. This usually happens when a method annotated with @SubBlock is invoked without parent(by using VFLStarter). Most of the time this should be okay.");
            return;
        }
        Log.INSTANCE.close(endMsg);
        BlockContext popped = loggerCtxStack.get().pop();
        log.debug("Popped current context : {}-{} for thread {}", popped.blockInfo.getBlockName(), Util.TrimId(popped.blockInfo.getId()), Util.GetThreadInfo());

        if (GetCurrentBlockContext() == null) {
            log.debug("Thread '{}' cleaned: popped first context '{}-{}'.",
                    Util.GetThreadInfo(),
                    popped.blockInfo.getBlockName(),
                    Util.TrimId(popped.blockInfo.getId()));

            CleanThreadVariables();
        }
    }

    static void PushBlockToThreadLogStack(Block subBlock) {
        loggerCtxStack.get().push(new BlockContext(subBlock, false));
    }
}