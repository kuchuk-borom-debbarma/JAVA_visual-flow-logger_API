package dev.kuku.vfl;

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

    public void runBlockAndStay(String blockName, String message, String blockEndMsg, Consumer<BlockLogger> fn) {
        internalCoreLogger.logWithoutResult(blockName, message, blockEndMsg, fn, false);
    }

    private static class InternalCoreLogger {
        private final BlockLogger blockLogger;

        private InternalCoreLogger(BlockLogger blockLogger) {
            this.blockLogger = blockLogger;
        }

        public void addMessageLog(String message, VflLogType logType, boolean moveForward) {
            ensureStartLogCreated();

            String logId = UUID.randomUUID().toString();
            var messageLog = new LogData(
                    logId,
                    blockLogger.blockData.getId(),
                    blockLogger.currentLogId,
                    logType,
                    message,
                    null,
                    Instant.now().toEpochMilli());
            blockLogger.buffer.pushLogToBuffer(messageLog);
            if (moveForward) {
                blockLogger.currentLogId = logId;
            }
        }

        public <T> T logWithResult(String blockName,
                                   String message,
                                   Function<T, String> endMessage,
                                   Function<BlockLogger, T> process,
                                   boolean moveForward) {
            try {
                Objects.requireNonNull(process, "Process cannot be null");
                Objects.requireNonNull(blockName, "Block name cannot be null");

                ensureStartLogCreated();

                String subBlockId = UUID.randomUUID().toString();
                String subBlockStartLogId = UUID.randomUUID().toString();

                BlockData subBlock = new BlockData(
                        subBlockId,
                        blockLogger.blockData.getId(),
                        blockName);
                blockLogger.buffer.pushBlockToBuffer(subBlock);

                LogData subBlockLog = new LogData(
                        subBlockStartLogId,
                        blockLogger.blockData.getId(),
                        blockLogger.currentLogId,
                        VflLogType.SUB_BLOCK_START,
                        message,
                        subBlockId,
                        Instant.now().toEpochMilli());
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
                    var endLog = new LogData(
                            endLogId,
                            subBlockId,
                            subBlockStartLogId,
                            VflLogType.BLOCK_END,
                            executeEndMessageFn(endMessage, null),
                            null,
                            Instant.now().toEpochMilli());
                    blockLogger.buffer.pushLogToBuffer(endLog);
                    throw e;
                }

                String endLogId = UUID.randomUUID().toString();
                var endLog = new LogData(
                        endLogId,
                        subBlockId,
                        subBlockStartLogId,
                        VflLogType.BLOCK_END,
                        executeEndMessageFn(endMessage, result),
                        null,
                        Instant.now().toEpochMilli());
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
                return Objects.requireNonNullElse(msg, "Processed end message is null");
            } catch (Exception e) {
                return "Error when processing End Message : " + e.getMessage();
            }
        }

        private void createStartLog() {
            String startLogId = UUID.randomUUID().toString();
            LogData blockStartLog = new LogData(
                    startLogId,
                    blockLogger.blockData.getId(),
                    null,
                    VflLogType.BLOCK_START,
                    null,
                    null,
                    Instant.now().toEpochMilli()
            );
            blockLogger.buffer.pushLogToBuffer(blockStartLog);
        }
    }
}
