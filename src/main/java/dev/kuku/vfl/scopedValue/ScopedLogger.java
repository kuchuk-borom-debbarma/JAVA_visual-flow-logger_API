package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Function;

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

    private <R> R toBeCalledFn(Callable<R> callable, Function<R, String> endMessageFn) {
        R result = null;
        try {
            result = callable.call();
        } catch (Exception e) {
            ScopedLogger.get().error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
        } finally {
            String endMessage = null;
            if (endMessageFn != null) {
                try {
                    endMessage = endMessageFn.apply(result);
                } catch (Exception e) {
                    endMessage = "Error processing End Message : " + String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage());
                }
            }
            ScopedLogger.get().closeBlock(endMessage);
        }
        return result;
    }

    private void run(String blockName, String message, Runnable runnable, boolean stay) {
        //Create sub block and push it
        String subBlockId = generateUID();
        var sbd = createAndPushBlockData(subBlockId, blockName);
        //Create sub block start log and push it
        var subBLockStartLog = createAndPushLogData(message, VflLogType.SUB_BLOCK_START, subBlockId);
        //Create the sub block logger data for sub block
        var subBlockLogger = new ScopedLoggerData(sbd, scopedBlockData.get().buffer);
        //Stay or move
        if (!stay) {
            scopedBlockData.get().currentLog = subBLockStartLog;
        }
        //Run runnable within new bound. This runnable will get its scope bound data
        ScopedValue.where(scopedBlockData, subBlockLogger)
                .run(() -> toBeCalledFn(() -> runnable, null));
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
        var endLog = createAndPushLogData(endMessage, VflLogType.BLOCK_END, null);
        scopedBlockData.get().buffer.pushLogToBuffer(endLog);
    }
}