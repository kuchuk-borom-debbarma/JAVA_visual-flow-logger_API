package dev.kuku.vfl.impl.annotation;

import dev.kuku.vfl.core.dtos.BlockContext;
import dev.kuku.vfl.core.helpers.VFLHelper;
import dev.kuku.vfl.core.models.Block;
import lombok.extern.slf4j.Slf4j;

import java.util.Stack;

/**
 * Thread-local stack manager for VFL {@link BlockContext}s.
 *
 * <p>This class maintains the execution context for the current thread’s active VFL blocks.
 * Each thread has its own stack of {@link BlockContext} objects, representing
 * the nested block hierarchy during the trace.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Push new block contexts when entering a traced block</li>
 *   <li>Pop and close contexts when leaving a traced block</li>
 *   <li>Clear the thread-local stack entirely when it becomes empty</li>
 *   <li>Provide quick access to the current (top) block context</li>
 * </ul>
 *
 * <p><b>Lifecycle Notes:</b>
 * <ul>
 *   <li>The stack grows as methods annotated with {@code @SubBlock} (or created via {@link VFLStarter}) are entered.</li>
 *   <li>Each push <b>must</b> be matched with a pop to avoid memory leaks and incorrect trace linking.</li>
 *   <li>If a pop is attempted without any active block, a warning is logged because it usually means
 *       a traced method was executed without a proper parent block context.</li>
 * </ul>
 *
 * <p>This is an <b>internal utility</b> — not designed for direct use by end‑users of the VFL library.
 */
@Slf4j
class ThreadContextManager {

    /** Per-thread stack holding nested block execution contexts */
    static final ThreadLocal<Stack<BlockContext>> loggerCtxStack = new ThreadLocal<>();

    /**
     * Get the current (top) block context for this thread.
     *
     * @return current {@link BlockContext} at the top of the stack, or {@code null} if none exists.
     */
    static BlockContext GetCurrentBlockContext() {
        if (loggerCtxStack.get() == null || loggerCtxStack.get().isEmpty()) return null;
        return loggerCtxStack.get().peek();
    }

    /**
     * Pop and close the current block context for this thread.
     *
     * <p>Performs the following:
     * <ol>
     *   <li>Checks if there is an active context; logs a warning if not found</li>
     *   <li>Closes the current block via {@link Log#close(String)}</li>
     *   <li>Pops the context from the stack</li>
     *   <li>If the stack becomes empty, removes the ThreadLocal entirely</li>
     * </ol>
     *
     * @param endMsg optional message passed to {@link Log#close(String)}
     */
    static void PopCurrentStack(String endMsg) {
        if (GetCurrentBlockContext() == null) {
            log.warn("Failed to close current context : Logger stack is empty or null. "
                    + "This usually happens when a method annotated with @SubBlock is invoked "
                    + "without a parent (via VFLStarter). Usually this can be ignored.");
            return;
        }
        Log.INSTANCE.close(endMsg);
        BlockContext popped = loggerCtxStack.get().pop();
        log.debug("Popped current context : {}-{} for thread {}",
                popped.blockInfo.getBlockName(),
                VFLHelper.TrimId(popped.blockInfo.getId()),
                VFLHelper.GetThreadInfo());

        if (loggerCtxStack.get().isEmpty()) {
            log.debug("LoggerCtxStack is empty for thread {}, Removing thread variable",
                    VFLHelper.GetThreadInfo());
            loggerCtxStack.remove();
        }
    }

    /**
     * Push a new {@link Block} onto the current thread's context stack.
     *
     * <p>Wraps the block inside a new {@link BlockContext} before pushing.
     * If the stack is not yet created for this thread, it initializes it.
     *
     * @param subBlock the new block to push as the current context
     */
    static void PushBlockToThreadLogStack(Block subBlock) {
        if (GetCurrentBlockContext() == null) {
            loggerCtxStack.set(new Stack<>());
        }
        loggerCtxStack.get().push(new BlockContext(subBlock));
    }
}
