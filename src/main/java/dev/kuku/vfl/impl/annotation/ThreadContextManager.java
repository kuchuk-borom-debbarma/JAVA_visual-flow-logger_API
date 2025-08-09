package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.Util;
import dev.kuku.vfl.core.models.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Stack;

/**
 * Manager thread local context
 */
public class ThreadContextManager {

    static final Logger log = LoggerFactory.getLogger(ThreadContextManager.class);
    static final ThreadLocal<Stack<BlockContext>> loggerCtxStack = new ThreadLocal<>();
    static final ThreadLocal<SpawnedThreadContext> spawnedThreadContext = new ThreadLocal<>();
    static VFLBuffer AnnotationBuffer;

    static void CleanThreadVariables() {
        loggerCtxStack.remove();
        spawnedThreadContext.remove();
    }

    static void InitializeStackWithBlock(Block block) {
        loggerCtxStack.set(new Stack<>());
        loggerCtxStack.get().push(new BlockContext(block));
    }

    public static void InitializeSpawnedThreadContext(SpawnedThreadContext spawnedContext) {
        CleanThreadVariables();
        spawnedThreadContext.set(spawnedContext);
    }

    static BlockContext GetCurrentBlockContext() {
        if (loggerCtxStack.get() == null) return null;
        return loggerCtxStack.get().peek();
    }

    public static void PopCurrentContext(String endMsg) {
        if (loggerCtxStack.get() == null) {
            log.warn("Failed to close current context : Logger stack is null");
            return;
        }
        if (loggerCtxStack.get().isEmpty()) {
            log.warn("Failed to close current context : Logger stack is empty");
            return;
        }

        BlockContext popped = loggerCtxStack.get().pop();
        log.debug("Popped current context : {}-{} for thread {}", popped.blockInfo.getBlockName(), Util.TrimId(popped.blockInfo.getId()), Util.GetThreadInfo());

        //TODO Consider lambda started blocks too
        if (GetCurrentBlockContext() == null) {
            log.debug("Thread '{}' cleaned: popped first context '{}-{}'.",
                    Util.GetThreadInfo(),
                    popped.blockInfo.getBlockName(),
                    Util.TrimId(popped.blockInfo.getId()));

            CleanThreadVariables();
        }
    }

    public static boolean IsSpawnedThread() {
        return spawnedThreadContext.get() != null;
    }

    public static void PushBlockToThreadLogStack(Block subBlock) {
        loggerCtxStack.get().push(new BlockContext(subBlock));
    }
}