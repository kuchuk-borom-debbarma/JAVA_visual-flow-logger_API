package dev.kuku.vfl.threadLocal;

import dev.kuku.vfl.core.VFL;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.VFLBlockContext;
import dev.kuku.vfl.core.models.VflLogType;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import static dev.kuku.vfl.core.util.HelperUtil.generateUID;

class ThreadLocaVFL extends VFL implements IThreadLocal {
    static final ThreadLocal<ThreadLocaVFL> THREAD_LOCAL_VFL = new ThreadLocal<>();

    /**
     * Initialize a new root logger at the current thread.
     */
    public static void init(String blockName, VFLBuffer buffer) {
        var currentThreadVFL = THREAD_LOCAL_VFL.get();
        if (currentThreadVFL != null) {
            throw new IllegalStateException("ThreadLocal VFL already initialised");
        }
        var ctx = new VFLBlockContext(new BlockData(generateUID(), null, blockName), buffer);
        THREAD_LOCAL_VFL.set(new ThreadLocaVFL(ctx));
    }

    private static void copy(ThreadLocaVFL toCopy) {
        THREAD_LOCAL_VFL.set(toCopy);
    }

    public static IThreadLocal get() {
        var current = THREAD_LOCAL_VFL.get();
        if (current == null) {
            throw new NullPointerException("ThreadLocal VFL has not been initialized. Please run " + ThreadLocaVFL.class + ".init(a,b) to start a root logger");
        }
        return current;
    }

    private ThreadLocaVFL(VFLBlockContext context) {
        super(context);
    }

    @Override
    public <R> CompletableFuture<R> callAsync(String blockName, String message, Callable<R> callable, Function<R, String> endMsgFn, Executor executor) {
        ensureBlockStarted();
        String subBlockId = generateUID();
        var createdBlock = new BlockData(generateUID(), this.blockContext.blockInfo.getId(), blockName);
        blockContext.buffer.pushBlockToBuffer(createdBlock);
        var createdLog = createLogAndPush(VflLogType.SUB_BLOCK_START, message, subBlockId);
        var current = ThreadLocaVFL.THREAD_LOCAL_VFL.get();
        return CompletableFuture.supplyAsync(() -> {
            try {
                var ctx = new VFLBlockContext(new BlockData(subBlockId, current.blockContext.blockInfo.getId(), blockName), current.blockContext.buffer);
                THREAD_LOCAL_VFL.set(new ThreadLocaVFL(ctx));
                //TODO
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {

            }
            return null;
        }, executor);
    }
}