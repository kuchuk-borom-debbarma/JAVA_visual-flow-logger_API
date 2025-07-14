package dev.kuku.vfl.core.logger;

import dev.kuku.vfl.buffer.VFLBuffer;
import dev.kuku.vfl.models.BlockData;
import dev.kuku.vfl.models.LogData;
import dev.kuku.vfl.models.VflLogType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

public class BlockLogger {
    private final VFLBuffer buffer;
    private final BlockData blockData;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final InternalCoreLogger internalCoreLogger = new InternalCoreLogger(this);
    private String currentLogId = null;

    public BlockLogger(BlockData blockData, VFLBuffer buffer) {
        this.buffer = buffer;
        this.blockData = blockData;
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

    /**
     * Create a sub-block logger instance and return it. It can't have an ending message as it is not possible to determine the end of the function. <br>
     * The end message has to be specified by invoking a method.
     *
     * @param blockName name of the block
     * @param message   message for the sub block start
     * @return sub block logger instance
     */
    //TODO child class that has function for end message
    public SubBlockLogger createSubBlockLogger(String blockName, String message) {
        return this.internalCoreLogger.createSubBlockLogger(blockName, message);
    }

    private static class InternalCoreLogger {
        private final BlockLogger blockLogger;

        private InternalCoreLogger(BlockLogger blockLogger) {
            this.blockLogger = blockLogger;
        }

        private SubBlockLogger createSubBlockLogger(String blockName, String message) {
            //TODO reduce code duplication
            ensureStartLogCreated();

            String subBlockId = UUID.randomUUID().toString();
            String subBlockStartLogId = UUID.randomUUID().toString();

            BlockData subBlock = new BlockData(subBlockId, blockLogger.blockData.getId(), blockName);
            blockLogger.buffer.pushBlockToBuffer(subBlock);

            LogData subBlockLog = new LogData(subBlockStartLogId, blockLogger.blockData.getId(), blockLogger.currentLogId, VflLogType.SUB_BLOCK_START, message, subBlockId, Instant.now().toEpochMilli());
            blockLogger.buffer.pushLogToBuffer(subBlockLog);
            return new SubBlockLogger(blockLogger.blockData, subBlock, blockLogger.buffer);
        }

        private void addMessageLog(String message, VflLogType logType, boolean moveForward) {
            ensureStartLogCreated();

            String logId = UUID.randomUUID().toString();
            var messageLog = new LogData(logId, blockLogger.blockData.getId(), blockLogger.currentLogId, logType, message, null, Instant.now().toEpochMilli());
            blockLogger.buffer.pushLogToBuffer(messageLog);
            if (moveForward) {
                blockLogger.currentLogId = logId;
            }
        }

        private <T> T logWithResult(String blockName, String message, Function<T, String> endMessage, Function<BlockLogger, T> process, boolean moveForward) {
            try {
                Objects.requireNonNull(process, "Process cannot be null");
                Objects.requireNonNull(blockName, "Block name cannot be null");

                ensureStartLogCreated();

                String subBlockId = UUID.randomUUID().toString();
                String subBlockStartLogId = UUID.randomUUID().toString();

                BlockData subBlock = new BlockData(subBlockId, blockLogger.blockData.getId(), blockName);
                blockLogger.buffer.pushBlockToBuffer(subBlock);

                LogData subBlockLog = new LogData(subBlockStartLogId, blockLogger.blockData.getId(), blockLogger.currentLogId, VflLogType.SUB_BLOCK_START, message, subBlockId, Instant.now().toEpochMilli());
                blockLogger.buffer.pushLogToBuffer(subBlockLog);

                if (moveForward) {
                    blockLogger.currentLogId = subBlockStartLogId;
                }

                BlockLogger subProcessBlockLogger = new BlockLogger(subBlock, blockLogger.buffer);
                T result;
                try {
                    result = process.apply(subProcessBlockLogger);
                } catch (Exception e) {
                    subProcessBlockLogger.internalCoreLogger.addMessageLog("Exception " + e.getMessage(), VflLogType.EXCEPTION, true);
                    String endLogId = UUID.randomUUID().toString();
                    /*
                    End log is not stored as a log of a block.
                    It is used to update the block's finishing time.
                     */
                    var endLog = new LogData(endLogId, subBlockId, subBlockStartLogId, VflLogType.BLOCK_END, executeEndMessageFn(endMessage, null), null, Instant.now().toEpochMilli());
                    blockLogger.buffer.pushLogToBuffer(endLog);
                    throw e;
                }

                String endLogId = UUID.randomUUID().toString();
                var endLog = new LogData(endLogId, subBlockId, subBlockStartLogId, VflLogType.BLOCK_END, executeEndMessageFn(endMessage, result), null, Instant.now().toEpochMilli());
                blockLogger.buffer.pushLogToBuffer(endLog);
                return result;
            } catch (Exception e) {
                addMessageLog("Exception when trying to run sub-block operation" + e.getMessage(), VflLogType.EXCEPTION, true);
                throw e;
            }
        }

        public void logWithoutResult(String blockName, String message, String endMessage, Consumer<BlockLogger> process, boolean moveForward) {
            Function<BlockLogger, Void> a = blockLogger -> {
                process.accept(blockLogger);
                return null;
            };
            this.logWithResult(blockName, message, (_) -> endMessage, a, moveForward);
        }

        private void ensureStartLogCreated() {
            if (blockLogger.isInitialized.compareAndSet(false, true)) {
                createStartLog();
            }
        }

        private <T> String executeEndMessageFn(Function<T, String> endMessage, T result) {
            if (endMessage == null) return null;
            try {
                String msg = endMessage.apply(result);
                return msg;
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
            LogData blockStartLog = new LogData(startLogId, blockLogger.blockData.getId(), null, VflLogType.BLOCK_START, null, null, Instant.now().toEpochMilli());
            blockLogger.buffer.pushLogToBuffer(blockStartLog);
        }
    }
}
