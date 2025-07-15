package dev.kuku.vfl.core.logger;

import dev.kuku.vfl.core.buffer.VFLBuffer;
import dev.kuku.vfl.core.models.BlockData;
import dev.kuku.vfl.core.models.LogData;
import dev.kuku.vfl.core.models.VflLogType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class BlockLogger implements AutoCloseable {
    protected final InternalCoreLogger internalCoreLogger;

    public BlockLogger(BlockData blockData, VFLBuffer buffer) {
        internalCoreLogger = new InternalCoreLogger(buffer, blockData);
    }

    public void message(String message) {
        internalCoreLogger.addMessageLog(message, VflLogType.MESSAGE, true);
    }

    public void messageAndStay(String message) {
        internalCoreLogger.addMessageLog(message, VflLogType.MESSAGE, false);
    }

    public void error(String message) {
        internalCoreLogger.addMessageLog(message, VflLogType.EXCEPTION, true);
    }

    public void errorAndStay(String message) {
        internalCoreLogger.addMessageLog(message, VflLogType.EXCEPTION, false);
    }

    public <T> T runBlockResult(String blockName, String message, Function<T, String> blockEndMsg, Function<BlockLogger, T> fn) {
        return internalCoreLogger.logWithResult(blockName, message, blockEndMsg, fn, true);
    }

    public <T> T runBlockResultAndStay(String blockName, String message, Function<T, String> blockEndMessage, Function<BlockLogger, T> fn) {
        return internalCoreLogger.logWithResult(blockName, message, blockEndMessage, fn, false);
    }

    public void runBlock(String blockName, String message, String blockEndMsg, Consumer<BlockLogger> fn) {
        internalCoreLogger.logWithoutResult(blockName, message, blockEndMsg, fn, true);
    }

    public void runBlock(String blockName, Consumer<BlockLogger> fn) {
        internalCoreLogger.logWithoutResult(blockName, null, null, fn, true);
    }

    public void runBlockAndStay(String blockName, String message, String blockEndMsg, Consumer<BlockLogger> fn) {
        internalCoreLogger.logWithoutResult(blockName, message, blockEndMsg, fn, false);
    }

    public void runBlockAndStay(String blockName, Consumer<BlockLogger> fn) {
        internalCoreLogger.logWithoutResult(blockName, null, null, fn, false);
    }

    @Override
    public void close() {
        //TODO close. use a boolean to determine if it's already closed or not. Atomic boolean or synchnorised
    }

    protected static class InternalCoreLogger {
        protected final BlockData blockData;
        private final VFLBuffer buffer;
        private final AtomicBoolean isInitialized = new AtomicBoolean(false);
        private String currentLogId = null;

        private InternalCoreLogger(VFLBuffer buffer, BlockData blockData) {
            this.buffer = buffer;
            this.blockData = blockData;
        }

        private BlockData createBlockDataAndPush(String subBlockId, String blockName) {
            BlockData bd = new BlockData(subBlockId, this.blockData.getId(), blockName);
            this.buffer.pushBlockToBuffer(bd);
            return bd;
        }

        protected LogData createLogDataAndPush(String logId,
                                               String blockId,
                                               String parentLogId,
                                               VflLogType logType,
                                               String message,
                                               String referenceValue) {
            LogData ld = new LogData(logId, blockId, parentLogId, logType, message, referenceValue, Instant.now().toEpochMilli());
            this.buffer.pushLogToBuffer(ld);
            return ld;
        }

        private void addMessageLog(String message, VflLogType logType, boolean moveForward) {
            ensureStartLogCreated();
            String logId = UUID.randomUUID().toString();
            this.createLogDataAndPush(logId, this.blockData.getId(), this.currentLogId, logType, message, null);
            if (moveForward) {
                this.currentLogId = logId;
            }
        }

        private <T> T logWithResult(String blockName, String message, Function<T, String> endMessage, Function<BlockLogger, T> process, boolean moveForward) {
            try {
                Objects.requireNonNull(process, "Process cannot be null");
                Objects.requireNonNull(blockName, "Block name cannot be null");
                ensureStartLogCreated();
                String subBlockId = UUID.randomUUID().toString();
                BlockData subBlockData = this.createBlockDataAndPush(subBlockId, blockName);
                String subBlockStartLogId = UUID.randomUUID().toString();
                this.createLogDataAndPush(subBlockStartLogId,
                        this.blockData.getId(),
                        this.currentLogId,
                        VflLogType.SUB_BLOCK_START,
                        message,
                        subBlockId);
                if (moveForward) {
                    this.currentLogId = subBlockStartLogId;
                }
                BlockLogger subProcessBlockLogger = new BlockLogger(subBlockData, this.buffer);
                T result;
                String endLogId = UUID.randomUUID().toString();
                try {
                    result = process.apply(subProcessBlockLogger);
                } catch (Exception e) {
                    subProcessBlockLogger.internalCoreLogger.addMessageLog("Exception " + e.getClass() + " : " + e.getMessage(), VflLogType.EXCEPTION, true);
                    /*
                    End log is not stored as a log of a block.
                    It is used to update the block's finishing time.
                     */
                    this.createLogDataAndPush(endLogId,
                            subBlockId,
                            subBlockStartLogId,
                            VflLogType.BLOCK_END,
                            executeEndMessageFn(endMessage, null),
                            null);
                    throw e;
                }
                this.createLogDataAndPush(endLogId,
                        subBlockId,
                        subBlockStartLogId,
                        VflLogType.BLOCK_END,
                        executeEndMessageFn(endMessage, result),
                        null);
                return result;
            } catch (Exception e) {
                addMessageLog("Exception when trying to run sub-block operation. " + e.getClass() + " : " + e.getMessage(), VflLogType.EXCEPTION, true);
                throw e;
            }
        }

        public void logWithoutResult(String blockName, String message, String endMessage, Consumer<BlockLogger> process, boolean moveForward) {
            Function<BlockLogger, Void> fn = blockLogger -> {
                process.accept(blockLogger);
                return null;
            };
            this.logWithResult(blockName, message, (_) -> endMessage, fn, moveForward);
        }

        /**
         * Push BLOCK_START log to buffer if it has not been pushed.
         */
        private void ensureStartLogCreated() {
            if (this.isInitialized.compareAndSet(false, true)) {
                createStartLog();
            }
        }

        private <T> String executeEndMessageFn(Function<T, String> endMessage, T result) {
            if (endMessage == null) return null;
            try {
                return endMessage.apply(result);
            } catch (Exception e) {
                return "Error when processing End Message : " + e.getMessage();
            }
        }

        private void createStartLog() {
            /*
            Start log doesn't stored as a log of the block.
            Server processes it to update the block's start time
             */
            String startLogId = UUID.randomUUID().toString();
            this.createLogDataAndPush(startLogId,
                    this.blockData.getId(),
                    null,
                    VflLogType.BLOCK_START,
                    null,
                    null);
        }
    }
}
