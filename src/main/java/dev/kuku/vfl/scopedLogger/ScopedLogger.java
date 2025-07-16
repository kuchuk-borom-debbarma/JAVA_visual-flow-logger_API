package dev.kuku.vfl.scopedLogger;

import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;
import static dev.kuku.vfl.core.util.VFLUtil.blockFnHandler;
import static dev.kuku.vfl.scopedLogger.ScopedValueBlockContext.scopedBlockContext;

public class ScopedLogger implements BlockLog {
    private static ScopedLogger instance;

    private ScopedLogger() {
    }

    //Not possible to make it static since we implement interface, so we use singleton instead.
    public static ScopedLogger get() {
        if (!scopedBlockContext.isBound()) {
            throw new IllegalStateException("scopedBlockData is not within ScopedValue bound. Please use " + ScopedLoggerRunner.class.getName() + " to start a new scope.");
        }
        if (instance == null) {
            instance = new ScopedLogger();
        }
        return instance;
    }

    private void ensureBlockStarted() {
        if (scopedBlockContext.get().blockStarted.compareAndSet(false, true)) {
            createAndPushLogData(null, VflLogType.BLOCK_START, null);
        }
    }

    private LogData createAndPushLogData(String message, VflLogType logType, String referencedBlockId) {
        var ld = new LogData(generateUID(),
                scopedBlockContext.get().blockInfo.getId(),
                scopedBlockContext.get().currentLog == null ? null : scopedBlockContext.get().currentLog.getId(),
                logType,
                message,
                referencedBlockId,
                Instant.now().toEpochMilli());
        scopedBlockContext.get().buffer.pushLogToBuffer(ld);
        return ld;
    }

    private BlockData createAndPushBlockData(String id, String blockName) {
        var bd = new BlockData(id, scopedBlockContext.get().blockInfo.getId(), blockName);
        scopedBlockContext.get().buffer.pushBlockToBuffer(bd);
        return bd;
    }

    @Override
    public void text(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.MESSAGE, null);
    }

    @Override
    public void textHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.MESSAGE, null);

    }

    @Override
    public void warn(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void warnHere(String message) {
        ensureBlockStarted();
        createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void error(String message) {
        ensureBlockStarted();
        scopedBlockContext.get().currentLog = createAndPushLogData(message, VflLogType.EXCEPTION, null);
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
            scopedBlockContext.get().currentLog = subBLockStartLog;
        }
        //Create the subblock logger data for subblock
        ScopedBlockContext subBlockLoggerData = new ScopedBlockContext(sbd, scopedBlockContext.get().buffer);
        //Run runnable within new bound. This runnable will get its scope bound data
        ScopedValue.where(scopedBlockContext, subBlockLoggerData)
                .run(() -> {
                    try {
                        var blockData = ScopedLogger.get();
                        blockFnHandler(() -> {
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