package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;
import static dev.kuku.vfl.core.util.VFLUtil.toBeCalledFn;
import static dev.kuku.vfl.scopedValue.ScopedValueLoggerData.scopedBlockData;

public class ScopedLogger implements BlockLog {
    private static ScopedLogger instance;

    private ScopedLogger() {
    }

    //Not possible to make it static since we implement interface, so we use singleton instead.
    public static ScopedLogger get() {
        if (!scopedBlockData.isBound()) {
            throw new IllegalStateException("scopedBlockData is not bound. Please use " + ScopedLogStarter.class.getName() + " to start a new scope.");
        }
        if (instance == null) {
            instance = new ScopedLogger();
        }
        return instance;
    }

    private void ensureBlockStarted() {
        if (scopedBlockData.get().blockStarted.compareAndSet(false, true)) {
            createAndPushLogData(null, VflLogType.BLOCK_START, null);
        }
    }

    private LogData createAndPushLogData(String message, VflLogType logType, String referencedBlockId) {
        var ld = new LogData(generateUID(),
                scopedBlockData.get().blockInfo.getId(),
                scopedBlockData.get().currentLog == null ? null : scopedBlockData.get().currentLog.getId(),
                logType,
                message,
                referencedBlockId,
                Instant.now().toEpochMilli());
        scopedBlockData.get().buffer.pushLogToBuffer(ld);
        return ld;
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        var bd = new BlockData(id, scopedBlockData.get().blockInfo.getId(), blockName);
        scopedBlockData.get().buffer.pushBlockToBuffer(bd);
        return bd;
    }

    @Override
    public void text(String message) {
        ensureBlockStarted();
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.MESSAGE, null);
    }

    @Override
    public void textHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.MESSAGE, null);

    }

    @Override
    public void warn(String message) {
        ensureBlockStarted();
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void warnHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.EXCEPTION, null);
    }

    @Override
    public void errorHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.EXCEPTION, null);
    }

    private void run(String blockName, String message, Runnable runnable, boolean stay) {
        ensureBlockStarted();
        //Create subblock and push it
        String subBlockId = generateUID();
        var sbd = createAndPushBlockData(subBlockId, blockName);
        //Create subblock start log and push it
        var subBLockStartLog = createAndPushLogData(message, VflLogType.SUB_BLOCK_START, subBlockId);
        //Stay or move
        if (!stay) {
            scopedBlockData.get().currentLog = subBLockStartLog;
        }
        //Create the subblock logger data for subblock
        BoundedLogData subBlockLoggerData = new BoundedLogData(sbd, scopedBlockData.get().buffer);
        //Run runnable within new bound. This runnable will get its scope bound data
        ScopedValue.where(scopedBlockData, subBlockLoggerData)
                .run(() -> {
                    try {
                        var blockData = ScopedLogger.get();
                        toBeCalledFn(() -> {
                            runnable.run();
                            return null;
                        }, null, ScopedLogger.get());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public void run(String blockName, String message, Runnable runnable) {
        this.run(blockName, message, runnable, false);
    }

    @Override
    public void runHere(String blockName, String message, Runnable runnable) {
        this.run(blockName, message, runnable, true);
    }

    @Override
    public void closeBlock(String endMessage) {
        ensureBlockStarted();
        createAndPushLogData(endMessage, VflLogType.BLOCK_END, null);
    }
}