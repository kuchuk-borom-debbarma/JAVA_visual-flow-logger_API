package dev.kuku.vfl.core;


import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

class ScopedBlockLogImpl implements BlockLog {

    private final ScopedValue<BlockLog> s = ScopedValue.newInstance();
    private final AtomicBoolean blockStarted = new AtomicBoolean(false);
    private final VFLBuffer buffer;
    /// Info about this block
    private final BlockData blockInfo;
    private volatile LogData currentLog;

    public ScopedBlockLogImpl(VFLBuffer buffer, BlockData blockInfo) {
        this.buffer = buffer;
        this.blockInfo = blockInfo;
    }

    private void ensureBlockStarted() {
        if (this.blockStarted.compareAndSet(false, true)) {
            createAndPushLog(VflLogType.BLOCK_START, null);
        }
    }

    //----------------------
    private LogData createAndPushLog(VflLogType logType, String message, String referencedBlock) {
        LogData ld = new LogData(UUID.randomUUID().toString(), this.blockInfo.getId(), null, logType, message, referencedBlock, Instant.now().toEpochMilli());
        buffer.pushLogToBuffer(ld);
        return ld;
    }

    private LogData createAndPushLog(VflLogType logType, String message) {
        return createAndPushLog(logType, message, null);
    }

    private LogData createAndPushLog(VflLogType logType) {
        return createAndPushLog(logType, null);
    }

    //------------------------
    private BlockData createAndPushBlock(String blockName) {
        BlockData b = new BlockData(UUID.randomUUID().toString(), this.blockInfo.getId(), blockName);
        buffer.pushBlockToBuffer(b);
        return b;
    }

    @Override
    public void text(String message) {
        currentLog = createAndPushLog(VflLogType.MESSAGE, message);
    }

    @Override
    public void textHere(String message) {
        createAndPushLog(VflLogType.MESSAGE, message);
    }

    @Override
    public void warn(String message) {
        currentLog = createAndPushLog(VflLogType.WARN, message);
    }

    @Override
    public void warnHere(String message) {
        createAndPushLog(VflLogType.WARN, message);
    }

    @Override
    public void error(String message) {
        currentLog = createAndPushLog(VflLogType.EXCEPTION, message);
    }

    @Override
    public void errorHere(String message) {
        createAndPushLog(VflLogType.EXCEPTION, message);
    }

    @Override
    public void run(Runnable runnable, String blockName, String message) {
        BlockData subBlock = createAndPushBlock(blockName);
        currentLog = createAndPushLog(VflLogType.SUB_BLOCK_START, message, subBlock.getId());
        var subBlockLogger = new ScopedBlockLogImpl(this.buffer, subBlock);
        ScopedValue.where(s, subBlockLogger)
                .run(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        subBlockLogger.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                        throw e;
                    } finally {
                        createAndPushLog(VflLogType.BLOCK_END, null, subBlock.getId());
                    }
                });
    }

    @Override
    public void runHere(Runnable runnable, String blockName, String message) {
        BlockData subBlock = createAndPushBlock(blockName);
        createAndPushLog(VflLogType.SUB_BLOCK_START, message, subBlock.getId());
        var subBlockLogger = new ScopedBlockLogImpl(this.buffer, subBlock);
        ScopedValue.where(s, subBlockLogger)
                .run(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        subBlockLogger.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                        throw e;
                    } finally {
                        createAndPushLog(VflLogType.BLOCK_END, null, subBlock.getId());
                    }
                });
    }
}
