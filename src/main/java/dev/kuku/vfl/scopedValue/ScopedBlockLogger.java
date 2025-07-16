package dev.kuku.vfl.scopedValue;

import dev.kuku.vfl.core.BlockLog;
import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

//TODO this is good but we can make it much easier to understand by starting off with a start wrapper itself
public class ScopedBlockLogger implements BlockLog {
    private static final ScopedValue<BlockLog> s = ScopedValue.newInstance();
    private final AtomicBoolean blockStarted = new AtomicBoolean(false);
    private final VFLBuffer buffer;
    /// Info about this block
    private final BlockData blockInfo;
    private volatile LogData currentLog;

    public ScopedBlockLogger(VFLBuffer buffer, BlockData blockInfo) {
       //x\todo One way we can make this idiomatic yet good to use his by having a rapper start function, and it requires a subblock logger instance to work and they both have to be ecstatic
        this.buffer = buffer;
        this.blockInfo = blockInfo;
        //Push block info to buffer
        buffer.pushBlockToBuffer(blockInfo);
    }

    public void run(String blockName, VFLBuffer buffer, Runnable runnable) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(runnable);
        Objects.requireNonNull(blockName);
        var bl = new ScopedBlockLogger(buffer, new BlockData(UUID.randomUUID().toString(), null, blockName));
        ScopedValue.where(s, bl)
                .run(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        bl.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                    } finally {
                        bl.closeBlock();
                    }
                });
    }

    public <R> R call(String blockName, Function<R, String> endMessageFn, VFLBuffer buffer, Callable<R> callable) {
        Objects.requireNonNull(buffer);
        Objects.requireNonNull(callable);
        Objects.requireNonNull(blockName);
        var b = new ScopedBlockLogger(buffer, new BlockData(UUID.randomUUID().toString(), null, blockName));
        return ScopedValue.where(s, b)
                .call(() -> {
                    R result = null;
                    try {
                        result = callable.call();
                        return result;

                    } catch (Exception e) {
                        b.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));

                    } finally {
                        String endMessage = null;
                        if (endMessageFn != null) {
                            try {
                                endMessage = endMessageFn.apply(result);
                            } catch (Exception e) {
                                endMessage = "Failed to process End Message : " + e.getClass() + " : " + e.getMessage();
                            }
                        }
                        b.closeBlock(endMessage);
                    }
                });
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

    @Override
    public void text(String message) {
        BlockLog scopedLogger = s.get();
        if (scopedLogger != null && scopedLogger != this) {
            scopedLogger.textHere(message);
        } else {
            currentLog = createAndPushLog(VflLogType.MESSAGE, message);
        }
    }

    @Override
    public void textHere(String message) {
        createAndPushLog(VflLogType.MESSAGE, message);
    }

    @Override
    public void warn(String message) {
        BlockLog scopedLogger = s.get();
        if (scopedLogger != null && scopedLogger != this) {
            scopedLogger.warnHere(message);
        } else {
            currentLog = createAndPushLog(VflLogType.WARN, message);
        }
    }

    @Override
    public void warnHere(String message) {
        createAndPushLog(VflLogType.WARN, message);
    }

    @Override
    public void error(String message) {
        BlockLog scopedLogger = s.get();
        if (scopedLogger != null && scopedLogger != this) {
            scopedLogger.errorHere(message);
        } else {
            currentLog = createAndPushLog(VflLogType.EXCEPTION, message);
        }
    }

    @Override
    public void errorHere(String message) {
        createAndPushLog(VflLogType.EXCEPTION, message);
    }

    @Override
    public void run(Runnable runnable, String blockName, String message) {
        BlockData subBlock = new BlockData(UUID.randomUUID().toString(), this.blockInfo.getId(), blockName);
        var subBlockLogger = new ScopedBlockLogger(this.buffer, subBlock);
        currentLog = createAndPushLog(VflLogType.SUB_BLOCK_START, message, subBlock.getId());
        ScopedValue.where(s, subBlockLogger)
                .run(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        subBlockLogger.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                        throw e;
                    } finally {
                        subBlockLogger.closeBlock();
                    }
                });
    }

    @Override
    public void runHere(Runnable runnable, String blockName, String message) {
        BlockData subBlock = new BlockData(UUID.randomUUID().toString(), this.blockInfo.getId(), blockName);
        var subBlockLogger = new ScopedBlockLogger(this.buffer, subBlock);
        createAndPushLog(VflLogType.SUB_BLOCK_START, message, subBlock.getId());
        ScopedValue.where(s, subBlockLogger)
                .run(() -> {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        subBlockLogger.error(String.format("%s : %s", e.getClass().getSimpleName(), e.getMessage()));
                        throw e;
                    } finally {
                        subBlockLogger.closeBlock();
                    }
                });
    }

    @Override
    public void closeBlock(String endMessage) {
        createAndPushLog(VflLogType.BLOCK_END, null, this.blockInfo.getId());
    }
}