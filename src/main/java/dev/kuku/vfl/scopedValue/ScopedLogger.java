package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;

import static dev.kuku.vfl.core.util.VFLUtil.generateUID;
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
            scopedBlockData.get().buffer.pushBlockToBuffer(new LogData());
        }
    }

    private LogData createAndPushLogData(String message, VflLogType logType, String referencedBlockId) {
        var ld = new LogData(generateUID(), scopedBlockData.get().blockInfo.getId(), scopedBlockData.get().currentLog.getId(), logType, message, referencedBlockId, Instant.now().toEpochMilli());
        scopedBlockData.get().buffer.pushLogToBuffer(ld);
        return ld;
    }

    @Override
    public void text(String message) {
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.MESSAGE, null);
    }

    @Override
    public void textHere(String message) {
        createAndPushLogData(message, VflLogType.MESSAGE, null);

    }

    @Override
    public void warn(String message) {
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void warnHere(String message) {
        createAndPushLogData(message, VflLogType.WARN, null);
    }

    @Override
    public void error(String message) {
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.EXCEPTION, null);
    }

    @Override
    public void errorHere(String message) {
        createAndPushLogData(message, VflLogType.EXCEPTION, null);
    }

    @Override
    public void run(Runnable runnable, String blockName, String message) {
        String subBlockId = generateUID();
        var sbd = new BlockData(subBlockId, scopedBlockData.get().blockInfo.getId(), blockName);
        scopedBlockData.get().buffer.pushBlockToBuffer(sbd);
        scopedBlockData.get().currentLog = createAndPushLogData(message, VflLogType.SUB_BLOCK_START, subBlockId);
        ScopedValue.where(scopedBlockData, new ScopedLoggerData(sbd, scopedBlockData.get().buffer))
                .run(runnable);
    }

    @Override
    public void runHere(Runnable runnable, String blockName, String message) {
        String subBlockId = generateUID();
        var sbd = new BlockData(subBlockId, scopedBlockData.get().blockInfo.getId(), blockName);
        scopedBlockData.get().buffer.pushBlockToBuffer(sbd);
        createAndPushLogData(message, VflLogType.SUB_BLOCK_START, subBlockId);
        ScopedValue.where(scopedBlockData, new ScopedLoggerData(sbd, scopedBlockData.get().buffer))
                .run(runnable);
    }

    @Override
    public void closeBlock(String endMessage) {
        var endLog = createAndPushLogData(endMessage, VflLogType.BLOCK_END, null);
        scopedBlockData.get().buffer.pushLogToBuffer(endLog);
    }
}